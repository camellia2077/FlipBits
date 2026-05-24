#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/common/error_code.h"
#include "android_bag/common/types.h"
#include "android_bag/transport/decoder.h"
#include "android_bag/transport/follow.h"

namespace bag {

class IEncodeOperation {
 public:
    virtual ~IEncodeOperation() = default;

    virtual ErrorCode Run() = 0;
    virtual ErrorCode Pump(const EncodePumpBudget& budget, bool* out_did_progress) = 0;
    virtual ErrorCode Cancel() = 0;
    virtual EncodeWorkPlan WorkPlan() const = 0;
    virtual EncodeProgressSnapshot Snapshot() const = 0;
    virtual ErrorCode TakeResult(EncodedPcmFollowResult* out_result) = 0;
};

enum class TransportValidationIssue {
    kOk = 0,
    kInvalidSampleRate = 1,
    kInvalidFrameSamples = 2,
    kInvalidMode = 3,
    kProAsciiOnly = 4,
    kPayloadTooLarge = 5,
    kInvalidFlashSignalProfile = 6,
    kInvalidFlashVoicingFlavor = 7,
    kMiniMorseOnly = 8,
    kEmptyText = 9,
};

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config, std::string_view text);
TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode EncodeTextToPcm16WithFollowData(const CoreConfig& config,
                                          const std::string& text,
                                          EncodedPcmFollowResult* out_result);
ErrorCode EncodeTextToPcm16WithFollowData(const CoreConfig& config,
                                          const std::string& text,
                                          EncodedPcmFollowResult* out_result,
                                          const EncodeProgressSink* progress_sink);
ErrorCode BuildEncodeFollowData(const CoreConfig& config,
                                const std::string& text,
                                PayloadFollowData* out_follow_data,
                                TextFollowData* out_text_follow_data);
std::unique_ptr<IEncodeOperation> CreateEncodeOperation(const CoreConfig& config,
                                                        const std::string& text);
std::unique_ptr<ITransportDecoder> CreateTransportDecoder(const CoreConfig& config);

}  // namespace bag
