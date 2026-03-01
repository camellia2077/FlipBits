#include "bag_api.h"

#include <algorithm>
#include <memory>

#include "bag/common/config.h"
#include "bag/common/error_code.h"
#include "bag/common/types.h"
#include "bag/common/version.h"
#include "bag/pipeline/pipeline.h"

struct bag_decoder {
    std::unique_ptr<bag::IPipeline> pipeline;
};

namespace {
bag_error_code ToApiCode(bag::ErrorCode code) {
    switch (code) {
    case bag::ErrorCode::kOk:
        return BAG_OK;
    case bag::ErrorCode::kInvalidArgument:
        return BAG_INVALID_ARGUMENT;
    case bag::ErrorCode::kNotReady:
        return BAG_NOT_READY;
    case bag::ErrorCode::kNotImplemented:
        return BAG_NOT_IMPLEMENTED;
    case bag::ErrorCode::kInternal:
    default:
        return BAG_INTERNAL;
    }
}
}  // namespace

bag_error_code bag_create_decoder(const bag_decoder_config* config, bag_decoder** out_decoder) {
    if (config == nullptr || out_decoder == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    bag::CoreConfig core_config{};
    core_config.sample_rate_hz = config->sample_rate_hz;
    core_config.frame_samples = config->frame_samples;
    core_config.enable_diagnostics = config->enable_diagnostics != 0;
    core_config.reserved = config->reserved;

    auto* decoder = new bag_decoder{};
    decoder->pipeline = bag::CreatePipeline(core_config);
    if (!decoder->pipeline) {
        delete decoder;
        return BAG_INTERNAL;
    }

    *out_decoder = decoder;
    return BAG_OK;
}

void bag_destroy_decoder(bag_decoder* decoder) {
    delete decoder;
}

bag_error_code bag_push_pcm(bag_decoder* decoder,
                            const int16_t* samples,
                            size_t sample_count,
                            int64_t timestamp_ms) {
    if (decoder == nullptr || decoder->pipeline == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    bag::PcmBlock block{};
    block.samples = samples;
    block.sample_count = sample_count;
    block.timestamp_ms = timestamp_ms;
    return ToApiCode(decoder->pipeline->PushPcm(block));
}

bag_error_code bag_poll_result(bag_decoder* decoder, bag_text_result* out_result) {
    if (decoder == nullptr || decoder->pipeline == nullptr || out_result == nullptr) {
        return BAG_INVALID_ARGUMENT;
    }

    bag::TextResult result{};
    const bag_error_code code = ToApiCode(decoder->pipeline->PollTextResult(&result));
    if (code != BAG_OK) {
        out_result->text_size = 0;
        out_result->complete = 0;
        out_result->confidence = 0.0f;
        return code;
    }

    out_result->text_size = result.text.size();
    out_result->complete = result.complete ? 1 : 0;
    out_result->confidence = result.confidence;

    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
        const size_t copy_size = std::min(result.text.size(), out_result->buffer_size - 1);
        if (copy_size > 0) {
            std::copy_n(result.text.data(), copy_size, out_result->buffer);
        }
        out_result->buffer[copy_size] = '\0';
    }

    return BAG_OK;
}

void bag_reset(bag_decoder* decoder) {
    if (decoder == nullptr || decoder->pipeline == nullptr) {
        return;
    }
    decoder->pipeline->Reset();
}

const char* bag_core_version(void) {
    return bag::CoreVersion();
}
