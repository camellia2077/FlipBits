struct bag_decoder {
  std::unique_ptr<bag::ITransportDecoder> decoder;
};

struct bag_encode_job {
#if !FLIPBITS_ENABLE_ASYNC_ENCODE_JOB
  int unavailable = 0;
#else
  std::unique_ptr<bag::IEncodeOperation> operation;
#endif
};

struct bag_encode_operation {
  std::unique_ptr<bag::IEncodeOperation> operation;
};
