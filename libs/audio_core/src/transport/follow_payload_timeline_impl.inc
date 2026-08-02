UltraFrameSection UltraFrameSectionForByte(std::size_t frame_byte_index,
                                           std::size_t payload_size) {
  if (frame_byte_index < ultra::kMfsK16PreambleSymbolCount) {
    return UltraFrameSection::kPreamble;
  }
  if (frame_byte_index <
      payload_size - ultra::kMfsK16TailSymbolCount) {
    return UltraFrameSection::kPayload;
  }
  return UltraFrameSection::kTail;
}

std::vector<UltraFrameSymbolEntry> BuildUltraFrameTimeline(
    const std::vector<std::uint8_t>& raw_payload_bytes,
    const CoreConfig& core_config) {
  const ultra::Mfsk16Config config = ultra::MakeMfsk16Config(core_config);
  std::vector<std::uint8_t> symbols;
  if (ultra::EncodePayloadToSymbols(raw_payload_bytes, config, &symbols) !=
      ErrorCode::kOk) {
    return {};
  }

  std::vector<UltraFrameSymbolEntry> timeline;
  timeline.reserve(symbols.size());
  for (std::size_t symbol_index = 0; symbol_index < symbols.size();
       ++symbol_index) {
    const UltraFrameSection section = UltraFrameSectionForByte(
        symbol_index, symbols.size());
    const bool is_payload = section == UltraFrameSection::kPayload;
    const std::size_t active_begin = ultra::kMfsK16PreambleSymbolCount;
    const std::size_t active_end = symbols.size() -
                                    ultra::kMfsK16TailSymbolCount;
    const std::size_t active_index = symbol_index >= active_begin
                                         ? symbol_index - active_begin
                                         : 0;
    const std::size_t active_count = active_end - active_begin;
    const std::size_t payload_byte_index =
        is_payload && !raw_payload_bytes.empty()
            ? std::min(raw_payload_bytes.size() - 1,
                       active_index * raw_payload_bytes.size() /
                           std::max<std::size_t>(active_count, 1))
            : std::numeric_limits<std::size_t>::max();
    const std::uint8_t nibble =
        ultra::kMfsK16ToneToNibble[static_cast<std::size_t>(symbols[symbol_index])];
    const std::size_t start_sample =
        ultra::SymbolBoundarySample(config.sample_rate_hz, symbol_index,
                                    config.symbol_rate_baud);
    const std::size_t end_sample =
        ultra::SymbolBoundarySample(config.sample_rate_hz, symbol_index + 1U,
                                    config.symbol_rate_baud);
    if (end_sample <= start_sample) {
      return {};
    }
    timeline.push_back(UltraFrameSymbolEntry{
        .start_sample = start_sample,
        .sample_count = end_sample - start_sample,
        .frame_byte_index = symbol_index,
        .nibble_index_in_byte = 0,
        .nibble_value = static_cast<std::size_t>(nibble),
        .carrier_freq_hz =
            config.freqs_hz[static_cast<std::size_t>(symbols[symbol_index])],
        .section = section,
        .is_payload = is_payload,
        .payload_byte_index = payload_byte_index,
    });
  }
  return timeline;
}

PayloadFollowData BuildFlashPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  PayloadFollowData follow_data{};
  const flash::BfskConfig signal_config = flash::MakeBfskConfigForSignalProfile(
      config, config.flash_signal_profile);
  const flash::FlashPayloadLayout payload_layout =
      flash::BuildPayloadLayoutForVoicing(raw_payload_bytes, signal_config,
                                          config.flash_voicing_flavor);
  const flash::FlashVoicingConfig voicing_config =
      flash::MakeFormalVoicingConfigForFlavor(config,
                                              config.flash_voicing_flavor);
  const std::size_t total_pcm_sample_count =
      payload_layout.payload_sample_count +
      voicing_config.preamble_sample_count +
      voicing_config.epilogue_sample_count;
  const flash::FlashVoicingDescriptor descriptor =
      flash::DescribeVoicingOutput(total_pcm_sample_count, voicing_config);

  follow_data.raw_payload_bytes = raw_payload_bytes;
  follow_data.byte_timeline.reserve(raw_payload_bytes.size());
  follow_data.binary_group_timeline.reserve(raw_payload_bytes.size() *
                                            static_cast<std::size_t>(8));
  follow_data.payload_begin_sample = descriptor.leading_nonpayload_samples;
  follow_data.payload_sample_count = descriptor.payload_sample_count;
  follow_data.total_pcm_sample_count = total_pcm_sample_count;
  follow_data.available = true;

  // Byte spans include any cadence silence after the byte's final bit. That
  // keeps text lyrics aligned with what the listener experiences, while the
  // binary timeline below still publishes only voiced low/high bit chunks.
  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    std::size_t byte_begin_sample = std::numeric_limits<std::size_t>::max();
    std::size_t byte_end_sample = 0;
    for (const flash::FlashPayloadChunk& chunk : payload_layout.chunks) {
      if (chunk.byte_index != byte_index) {
        continue;
      }
      byte_begin_sample = std::min(byte_begin_sample, chunk.sample_offset);
      byte_end_sample =
          std::max(byte_end_sample, chunk.sample_offset + chunk.sample_count);
    }
    if (byte_begin_sample == std::numeric_limits<std::size_t>::max() ||
        byte_end_sample <= byte_begin_sample) {
      continue;
    }
    follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
        .start_sample =
            descriptor.leading_nonpayload_samples + byte_begin_sample,
        .sample_count = byte_end_sample - byte_begin_sample,
        .byte_index = byte_index,
    });
  }

  // Flash follow data is intentionally bit-granular. The UI uses bit_offset as
  // the source of truth for lyrics highlighting and for timeline-driven
  // low/high visuals; group_index is only the ordinal of this published follow
  // group.
  std::size_t bit_group_index = 0;
  for (const flash::FlashPayloadChunk& chunk : payload_layout.chunks) {
    if (chunk.kind != flash::FlashPayloadSegmentKind::kBit) {
      continue;
    }
    const std::size_t bit_offset =
        chunk.byte_index * static_cast<std::size_t>(8) +
        static_cast<std::size_t>(chunk.bit_index_in_byte);
    follow_data.binary_group_timeline.push_back(PayloadFollowBinaryGroupEntry{
        .start_sample =
            descriptor.leading_nonpayload_samples + chunk.sample_offset,
        .sample_count = chunk.sample_count,
        .group_index = bit_group_index,
        .bit_offset = bit_offset,
        .bit_count = 1,
        .carrier_freq_hz = chunk.carrier_freq_hz,
    });
    ++bit_group_index;
  }

  return follow_data;
}

PayloadFollowData BuildNibblePayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  if (config.mode == TransportMode::kUltra) {
    const auto timeline =
        BuildUltraFrameTimeline(raw_payload_bytes, config);
    if (timeline.empty()) {
      return {};
    }
    const std::size_t active_begin = ultra::kMfsK16PreambleSymbolCount;
    const std::size_t active_end = timeline.size() -
                                   ultra::kMfsK16TailSymbolCount;
    PayloadFollowData follow_data{};
    follow_data.raw_payload_bytes = raw_payload_bytes;
    follow_data.payload_begin_sample = timeline[active_begin].start_sample;
    follow_data.payload_sample_count =
        timeline[active_end - 1U].start_sample +
        timeline[active_end - 1U].sample_count -
        follow_data.payload_begin_sample;
    follow_data.total_pcm_sample_count =
        timeline.back().start_sample + timeline.back().sample_count;
    follow_data.ultra_frame_timeline = timeline;
    follow_data.available = true;
    follow_data.byte_timeline.reserve(raw_payload_bytes.size());
    for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
         ++byte_index) {
      const std::size_t begin = follow_data.payload_begin_sample +
                                byte_index * follow_data.payload_sample_count /
                                    std::max<std::size_t>(raw_payload_bytes.size(), 1);
      const std::size_t end = follow_data.payload_begin_sample +
                              (byte_index + 1) * follow_data.payload_sample_count /
                                  std::max<std::size_t>(raw_payload_bytes.size(), 1);
      follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
          .start_sample = begin,
          .sample_count = end - begin,
          .byte_index = byte_index,
      });
    }
    follow_data.binary_group_timeline.reserve(timeline.size());
    for (std::size_t group_index = 0; group_index < timeline.size();
         ++group_index) {
      const auto& symbol = timeline[group_index];
      follow_data.binary_group_timeline.push_back(
          PayloadFollowBinaryGroupEntry{
              .start_sample = symbol.start_sample,
              .sample_count = symbol.sample_count,
              .group_index = group_index,
              .bit_offset = group_index * 4,
              .bit_count = 4,
              .carrier_freq_hz = symbol.carrier_freq_hz,
          });
    }
    return follow_data;
  }

  PayloadFollowData follow_data{};
  const std::size_t symbol_sample_count =
      config.frame_samples > 0 ? static_cast<std::size_t>(config.frame_samples)
                               : static_cast<std::size_t>(0);
  const std::size_t symbol_count =
      raw_payload_bytes.size() * static_cast<std::size_t>(2);
  const bool is_ultra = false;
  const std::size_t frame_prefix_byte_count = 0;
  const std::size_t frame_byte_count = raw_payload_bytes.size();
  const std::size_t payload_begin_sample = frame_prefix_byte_count *
                                           static_cast<std::size_t>(2) *
                                           symbol_sample_count;
  const std::size_t payload_sample_count = symbol_count * symbol_sample_count;
  const std::size_t total_pcm_sample_count =
      frame_byte_count * static_cast<std::size_t>(2) * symbol_sample_count;

  follow_data.raw_payload_bytes = raw_payload_bytes;
  follow_data.byte_timeline.reserve(raw_payload_bytes.size());
  follow_data.binary_group_timeline.reserve(symbol_count);
  follow_data.payload_begin_sample = payload_begin_sample;
  follow_data.payload_sample_count = payload_sample_count;
  follow_data.total_pcm_sample_count = total_pcm_sample_count;
  follow_data.available = true;
  if (is_ultra) {
    follow_data.ultra_frame_timeline =
        BuildUltraFrameTimeline(raw_payload_bytes, config);
  }

  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
        .start_sample = payload_begin_sample + byte_index *
                                                   static_cast<std::size_t>(2) *
                                                   symbol_sample_count,
        .sample_count = static_cast<std::size_t>(2) * symbol_sample_count,
        .byte_index = byte_index,
    });
  }

  // Coded transports publish one follow group per nibble-sized symbol. Keeping
  // bit_offset/bit_count explicit lets UI code consume the same timeline shape
  // without assuming every group is a single bit.
  for (std::size_t group_index = 0; group_index < symbol_count; ++group_index) {
    const std::uint8_t payload_byte =
        raw_payload_bytes[group_index / static_cast<std::size_t>(2)];
    const std::uint8_t symbol =
        group_index % static_cast<std::size_t>(2) == 0
            ? static_cast<std::uint8_t>((payload_byte >> 4) & 0x0F)
            : static_cast<std::uint8_t>(payload_byte & 0x0F);
    follow_data.binary_group_timeline.push_back(PayloadFollowBinaryGroupEntry{
        .start_sample =
            payload_begin_sample + group_index * symbol_sample_count,
        .sample_count = symbol_sample_count,
        .group_index = group_index,
        .bit_offset = group_index * static_cast<std::size_t>(4),
        .bit_count = 4,
        .carrier_freq_hz =
                config.mode == TransportMode::kUltra
                ? ultra::MakeMfsk16Config(config).freqs_hz[
                      static_cast<std::size_t>(symbol)]
                : 0.0,
    });
  }

  return follow_data;
}

PayloadFollowData BuildMorsePayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  PayloadFollowData follow_data{};
  const std::size_t unit_sample_count =
      config.frame_samples > 0 ? static_cast<std::size_t>(config.frame_samples)
                               : static_cast<std::size_t>(0);
  std::size_t total_unit_count = 0;
  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    total_unit_count +=
        mini_mode::MorseUnitCountForPayloadByte(raw_payload_bytes[byte_index]);
    total_unit_count +=
        mini_mode::MorseTrailingGapUnits(raw_payload_bytes, byte_index);
  }
  const std::size_t total_pcm_sample_count =
      total_unit_count * unit_sample_count;

  follow_data.raw_payload_bytes = raw_payload_bytes;
  follow_data.byte_timeline.reserve(raw_payload_bytes.size());
  follow_data.binary_group_timeline.reserve(raw_payload_bytes.size() *
                                            static_cast<std::size_t>(4));
  follow_data.payload_begin_sample = 0;
  follow_data.payload_sample_count = total_pcm_sample_count;
  follow_data.total_pcm_sample_count = total_pcm_sample_count;
  follow_data.available = true;

  // Mini/Morse stores normalized characters as payload bytes, but the follow
  // groups are Morse tone elements. Protocol silence remains visible as gaps in
  // sample time and is not published as a dot/dash group.
  std::size_t sample_cursor = 0;
  std::size_t group_index = 0;
  for (std::size_t byte_index = 0; byte_index < raw_payload_bytes.size();
       ++byte_index) {
    const std::size_t byte_begin_sample = sample_cursor;
    const char ch = static_cast<char>(raw_payload_bytes[byte_index]);
    if (ch == ' ') {
      sample_cursor += mini_mode::kMorseWordGapUnits * unit_sample_count;
    } else {
      const std::string_view pattern = mini_mode::MorsePatternForChar(
          static_cast<char>(raw_payload_bytes[byte_index]));
      for (std::size_t pattern_index = 0; pattern_index < pattern.size();
           ++pattern_index) {
        const std::size_t tone_units = pattern[pattern_index] == '-'
                                           ? mini_mode::kMorseDashUnits
                                           : mini_mode::kMorseDotUnits;
        follow_data.binary_group_timeline.push_back(
            PayloadFollowBinaryGroupEntry{
                .start_sample = sample_cursor,
                .sample_count = tone_units * unit_sample_count,
                .group_index = group_index,
                .bit_offset =
                    byte_index * static_cast<std::size_t>(8) + pattern_index,
                .bit_count = 1,
            });
        ++group_index;
        sample_cursor += tone_units * unit_sample_count;
        if (pattern_index + 1 < pattern.size()) {
          sample_cursor += mini_mode::kMorseElementGapUnits * unit_sample_count;
        }
      }
      sample_cursor +=
          mini_mode::MorseTrailingGapUnits(raw_payload_bytes, byte_index) *
          unit_sample_count;
    }
    const std::size_t byte_units =
        mini_mode::MorseUnitCountForPayloadByte(raw_payload_bytes[byte_index]) +
        mini_mode::MorseTrailingGapUnits(raw_payload_bytes, byte_index);
    const std::size_t byte_samples = byte_units * unit_sample_count;
    follow_data.byte_timeline.push_back(PayloadFollowByteEntry{
        .start_sample = byte_begin_sample,
        .sample_count = byte_samples,
        .byte_index = byte_index,
    });
  }

  return follow_data;
}
