PayloadFollowData BuildPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes) {
  if (config.sample_rate_hz <= 0 || config.frame_samples <= 0 ||
      !IsValidTransportMode(config.mode) ||
      !IsValidFlashSignalProfile(config.flash_signal_profile) ||
      !IsValidFlashVoicingFlavor(config.flash_voicing_flavor)) {
    return {};
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return BuildFlashPayloadFollowData(config, raw_payload_bytes);
    case TransportMode::kPro:
    case TransportMode::kUltra:
      return BuildNibblePayloadFollowData(config, raw_payload_bytes);
    case TransportMode::kMini:
      return BuildMorsePayloadFollowData(config, raw_payload_bytes);
    default:
      return {};
  }
}

TextFollowData BuildTextFollowData(const PayloadFollowData& payload_follow_data,
                                   std::string_view text) {
  TextFollowData text_follow_data{};
  if (!payload_follow_data.available) {
    return text_follow_data;
  }

  const std::vector<TextTokenSpec> token_specs = TokenizeTextForFollow(text);
  if (token_specs.empty()) {
    text_follow_data.available =
        text.empty() && payload_follow_data.raw_payload_bytes.empty();
    return text_follow_data;
  }

  text_follow_data.text_tokens.reserve(token_specs.size());
  text_follow_data.text_token_timeline.reserve(token_specs.size());
  text_follow_data.text_characters.reserve(text.size());
  text_follow_data.token_raw_segments.reserve(token_specs.size());
  text_follow_data.token_raw_display_units.reserve(
      payload_follow_data.byte_timeline.size());
  std::size_t byte_cursor = 0;
  const auto& byte_timeline = payload_follow_data.byte_timeline;
  for (std::size_t token_index = 0; token_index < token_specs.size();
       ++token_index) {
    const TextTokenSpec& token = token_specs[token_index];
    if (token.covered_byte_count == 0 || byte_cursor >= byte_timeline.size() ||
        byte_cursor + token.covered_byte_count > byte_timeline.size()) {
      return {};
    }
    const auto& first_byte = byte_timeline[byte_cursor];
    const auto& last_byte =
        byte_timeline[byte_cursor + token.covered_byte_count -
                      static_cast<std::size_t>(1)];
    const std::size_t token_begin_sample = first_byte.start_sample;
    const std::size_t token_end_sample =
        last_byte.start_sample + last_byte.sample_count;
    text_follow_data.text_tokens.push_back(token.display_text);
    text_follow_data.text_token_timeline.push_back(TextFollowTokenTimelineEntry{
        .start_sample = token_begin_sample,
        .sample_count = token_end_sample - token_begin_sample,
        .token_index = token_index,
    });
    text_follow_data.token_raw_segments.push_back(TextFollowRawSegmentEntry{
        .start_sample = token_begin_sample,
        .sample_count = token_end_sample - token_begin_sample,
        .token_index = token_index,
        .byte_offset = byte_cursor,
        .byte_count = token.covered_byte_count,
    });

    // Character mapping is the second layer of the contract. We decode only
    // the token's visible text into code points, publish those as formal
    // character entries, then walk the token-owned payload bytes in order to
    // annotate every byte with character-relative metadata.
    const auto visible_character_slices =
        DecodeUtf8CodePointSlices(token.display_text);
    const auto separator_character_slices =
        DecodeUtf8CodePointSlices(token.trailing_separator_text);

    std::size_t token_text_byte_offset = 0;
    std::size_t token_byte_index = 0;
    std::size_t character_index_within_token = 0;
    const auto classify_character_kind =
        [](std::uint32_t code_point) -> TextFollowCharacterKind {
      switch (code_point) {
        case 0x20:
        case 0x09:
        case 0x3000:
          return TextFollowCharacterKind::kSpace;
        case 0x0A:
        case 0x0D:
          return TextFollowCharacterKind::kNewline;
        default:
          return IsWhitespaceCodePoint(code_point)
                     ? TextFollowCharacterKind::kSeparatorOther
                     : TextFollowCharacterKind::kVisible;
      }
    };
    const auto append_character_bytes =
        [&](std::size_t byte_count, TextFollowCharacterKind kind,
            std::string_view character_text) -> bool {
      if (byte_count == 0 ||
          token_byte_index + byte_count > token.covered_byte_count) {
        return false;
      }

      const auto& first_character_byte =
          byte_timeline[byte_cursor + token_byte_index];
      const auto& last_character_byte =
          byte_timeline[byte_cursor + token_byte_index + byte_count -
                        static_cast<std::size_t>(1)];
      const std::size_t character_begin_sample =
          first_character_byte.start_sample;
      const std::size_t character_end_sample =
          last_character_byte.start_sample + last_character_byte.sample_count;
      text_follow_data.text_characters.push_back(TextFollowCharacterEntry{
          .start_sample = character_begin_sample,
          .sample_count = character_end_sample - character_begin_sample,
          .token_index = token_index,
          .character_index_within_token = character_index_within_token,
          .byte_index_within_token = token_byte_index,
          .byte_count = byte_count,
          .kind = kind,
          .text = std::string(character_text),
      });

      for (std::size_t byte_index_within_character = 0;
           byte_index_within_character < byte_count;
           ++byte_index_within_character) {
        const auto& byte_entry = byte_timeline[byte_cursor + token_byte_index +
                                               byte_index_within_character];
        text_follow_data.token_raw_display_units.push_back(
            TextFollowRawDisplayUnitEntry{
                .start_sample = byte_entry.start_sample,
                .sample_count = byte_entry.sample_count,
                .token_index = token_index,
                .byte_index_within_token =
                    token_byte_index + byte_index_within_character,
                .byte_offset = byte_entry.byte_index,
                .byte_count = 1,
                .character_index_within_token = character_index_within_token,
                .byte_index_within_character = byte_index_within_character,
                .character_byte_count = byte_count,
                .is_character_start = byte_index_within_character == 0,
                .is_character_end =
                    byte_index_within_character + 1 == byte_count,
            });
      }

      token_byte_index += byte_count;
      ++character_index_within_token;
      return true;
    };

    for (const auto& character_slice : visible_character_slices) {
      const std::size_t character_byte_count = character_slice.bytes.size();
      if (!append_character_bytes(character_byte_count,
                                  TextFollowCharacterKind::kVisible,
                                  character_slice.bytes)) {
        return {};
      }
      token_text_byte_offset += character_byte_count;
    }

    // Token-owned separators such as spaces and newlines are still part of the
    // token byte span for timing continuity. Publish them as formal character
    // entries so frontends can keep token text highlighting aligned with the
    // trailing byte span without re-parsing separator semantics on their own.
    for (const auto& separator_slice : separator_character_slices) {
      if (!append_character_bytes(
              separator_slice.bytes.size(),
              classify_character_kind(separator_slice.code_point),
              separator_slice.bytes)) {
        return {};
      }
    }

    // Defensive fallback for any remaining token-owned bytes. This keeps the
    // byte timeline total aligned with the payload even if future tokenization
    // rules attach non-visible bytes that are not modeled above yet.
    while (token_byte_index < token.covered_byte_count) {
      if (!append_character_bytes(static_cast<std::size_t>(1),
                                  TextFollowCharacterKind::kSeparatorOther,
                                  std::string_view{})) {
        return {};
      }
    }
    byte_cursor += token.covered_byte_count;
  }

  if (byte_cursor != byte_timeline.size()) {
    return {};
  }

  BuildLyricLineData(token_specs, &text_follow_data);
  text_follow_data.available = true;
  return text_follow_data;
}
