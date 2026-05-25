struct bag_decoder {
  std::unique_ptr<bag::ITransportDecoder> decoder;
};

struct bag_encode_operation {
  std::unique_ptr<bag::IEncodeOperation> operation;
};
