struct bag_decoder {
  std::unique_ptr<bag::ITransportDecoder> decoder;
};

struct bag_encode_operation {
  std::unique_ptr<bag::IEncodeOperation> operation;
};

struct bag_decode_operation {
  bag_decoder_config config{};
  std::vector<std::int16_t> pcm;
  std::unique_ptr<bag::ITransportDecoder> decoder;
  bag::DecodeResult result{};
  bag_decode_operation_state state = BAG_DECODE_OPERATION_QUEUED;
  bag_decode_operation_phase phase = BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT;
  float overall_progress_0_to_1 = 0.0f;
  float phase_progress_0_to_1 = 0.0f;
  std::uint64_t completed_work_units = 0;
  std::uint64_t phase_completed_work_units = 0;
  bag_error_code terminal_code = BAG_NOT_READY;
  std::size_t pushed_pcm_sample_count = 0;
  bool cancel_requested = false;
};
