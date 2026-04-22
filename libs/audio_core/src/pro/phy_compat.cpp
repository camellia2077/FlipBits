module;

import std;

module bag.pro.phy_compat;

import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.transport.follow;

namespace bag::pro {

using std::int16_t;
using std::uint8_t;

namespace {

class ProCompatDecoder final : public ITransportDecoder {
 public:
  explicit ProCompatDecoder(CoreConfig config) : config_(config) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (block.samples == nullptr || block.sample_count == 0) {
      return ErrorCode::kInvalidArgument;
    }
    buffered_pcm_.insert(buffered_pcm_.end(), block.samples,
                         block.samples + block.sample_count);
    has_pending_result_ = true;
    return ErrorCode::kOk;
  }

  ErrorCode PollDecodeResult(DecodeResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    if (!has_pending_result_ || buffered_pcm_.empty()) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = {};
      out_result->follow_available = false;
      out_result->text_follow_data = {};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    std::vector<uint8_t> frame_bytes;
    const ErrorCode frame_bytes_code =
        DecodePcm16ToFrameBytes(config_, buffered_pcm_, &frame_bytes);
    if (frame_bytes_code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = {};
      out_result->follow_available = false;
      out_result->text_follow_data = {};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      out_result->text_status = DecodeContentStatus::kInternalError;
      return ErrorCode::kInternal;
    }

    bag::transport::compat::DecodedFrame frame{};
    if (bag::transport::compat::DecodeFrame(frame_bytes, &frame) !=
            ErrorCode::kOk ||
        frame.mode != TransportMode::kPro) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = {};
      out_result->follow_available = false;
      out_result->text_follow_data = {};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      out_result->text_status = DecodeContentStatus::kInternalError;
      return ErrorCode::kInternal;
    }

    out_result->raw_payload_bytes = frame.payload;
    out_result->raw_payload_available = true;
    out_result->follow_data = BuildPayloadFollowData(config_, frame.payload);
    out_result->follow_available = out_result->follow_data.available;
    out_result->text.clear();
    if (DecodePayloadToText(frame.payload, &out_result->text) != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->text_status = DecodeContentStatus::kInvalidTextPayload;
      out_result->text_follow_data = {};
      out_result->text_follow_available = false;
    } else {
      out_result->text_status = DecodeContentStatus::kOk;
      out_result->text_follow_data =
          BuildTextFollowData(out_result->follow_data, out_result->text);
      out_result->text_follow_available =
          out_result->text_follow_data.available;
    }

    out_result->complete = true;
    out_result->confidence = 1.0f;
    out_result->mode = TransportMode::kPro;
    has_pending_result_ = false;
    return ErrorCode::kOk;
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    DecodeResult decode_result{};
    const ErrorCode code = PollDecodeResult(&decode_result);
    if (code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      return code;
    }
    if (decode_result.text_status != DecodeContentStatus::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kPro;
      return ErrorCode::kInternal;
    }
    out_result->text = decode_result.text;
    out_result->complete = decode_result.complete;
    out_result->confidence = decode_result.confidence;
    out_result->mode = decode_result.mode;
    return ErrorCode::kOk;
  }

  void Reset() override {
    buffered_pcm_.clear();
    has_pending_result_ = false;
  }

 private:
  CoreConfig config_;
  std::vector<int16_t> buffered_pcm_;
  bool has_pending_result_ = false;
};

}  // namespace

ErrorCode EncodeFrameBytesToPcm16(const CoreConfig& config,
                                  const std::vector<uint8_t>& frame_bytes,
                                  std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  try {
    *out_pcm = bag::flash::EncodeBytesToPcm16(
        frame_bytes, bag::flash::MakeBfskConfig(config));
    return ErrorCode::kOk;
  } catch (...) {
    return ErrorCode::kInvalidArgument;
  }
}

ErrorCode DecodePcm16ToFrameBytes(const CoreConfig& config,
                                  const std::vector<int16_t>& pcm,
                                  std::vector<uint8_t>* out_frame_bytes) {
  if (out_frame_bytes == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  try {
    *out_frame_bytes =
        bag::flash::DecodePcm16ToBytes(pcm, bag::flash::MakeBfskConfig(config));
    return ErrorCode::kOk;
  } catch (...) {
    return ErrorCode::kInvalidArgument;
  }
}

ErrorCode EncodeTextToPcm16Compat(const CoreConfig& config,
                                  const std::string& text,
                                  std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> payload;
  const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }

  std::vector<uint8_t> frame_bytes;
  const ErrorCode frame_code = bag::transport::compat::EncodeFrame(
      TransportMode::kPro, payload, &frame_bytes);
  if (frame_code != ErrorCode::kOk) {
    return frame_code;
  }

  return EncodeFrameBytesToPcm16(config, frame_bytes, out_pcm);
}

std::unique_ptr<ITransportDecoder> CreateCompatDecoder(
    const CoreConfig& config) {
  return std::make_unique<ProCompatDecoder>(config);
}

}  // namespace bag::pro
