void bag_free_pcm16_result(bag_pcm16_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
}

void bag_free_encode_result(bag_encode_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
  result->raw_bytes_hex_size = 0;
  result->raw_bits_binary_size = 0;
  result->raw_payload_available = 0;
  result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  ClearApiFollowData(&result->follow_data);
  ClearApiTextFollowData(&result->text_follow_data);
}
