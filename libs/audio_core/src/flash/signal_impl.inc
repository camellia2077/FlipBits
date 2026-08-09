namespace bag::flash {

std::vector<std::int16_t> EncodeBytesToPcm16(
    const std::vector<std::uint8_t>& bytes, const BfskConfig& config,
    const EncodeProgressSink* progress_sink, float progress_begin,
    float progress_end) {
  return EncodePayloadLayoutToPcm16(BuildPayloadLayout(bytes, config), config,
                                    progress_sink, progress_begin,
                                    progress_end);
}

}  // namespace bag::flash
