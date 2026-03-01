#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum bag_error_code {
    BAG_OK = 0,
    BAG_INVALID_ARGUMENT = 1,
    BAG_NOT_READY = 2,
    BAG_NOT_IMPLEMENTED = 3,
    BAG_INTERNAL = 4
} bag_error_code;

typedef struct bag_decoder bag_decoder;

typedef struct bag_decoder_config {
    int sample_rate_hz;
    int frame_samples;
    int enable_diagnostics;
    int reserved;
} bag_decoder_config;

typedef struct bag_text_result {
    char* buffer;
    size_t buffer_size;
    size_t text_size;
    int complete;
    float confidence;
} bag_text_result;

bag_error_code bag_create_decoder(const bag_decoder_config* config, bag_decoder** out_decoder);
void bag_destroy_decoder(bag_decoder* decoder);

bag_error_code bag_push_pcm(bag_decoder* decoder,
                            const int16_t* samples,
                            size_t sample_count,
                            int64_t timestamp_ms);

bag_error_code bag_poll_result(bag_decoder* decoder, bag_text_result* out_result);
void bag_reset(bag_decoder* decoder);
const char* bag_core_version(void);

#ifdef __cplusplus
}
#endif
