#pragma once

#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_utf8.h"
#include "test_vectors.h"

namespace api_tests {

struct DecodeResult {
  bag_error_code code = BAG_INTERNAL;
  std::string text;
  std::string raw_bytes_hex;
  std::string raw_bits_binary;
  bag_decode_content_status text_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  bool raw_payload_available = false;
  bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

bag_encoder_config MakeEncoderConfig(
    const test::ConfigCase& config_case,
    bag_transport_mode mode = BAG_TRANSPORT_FLASH,
    bag_flash_signal_profile flash_signal_profile =
        BAG_FLASH_SIGNAL_PROFILE_STANDARD,
    bag_flash_voicing_flavor flash_voicing_flavor =
        BAG_FLASH_VOICING_FLAVOR_STANDARD);
bag_decoder_config MakeDecoderConfig(
    const test::ConfigCase& config_case,
    bag_transport_mode mode = BAG_TRANSPORT_FLASH,
    bag_flash_signal_profile flash_signal_profile =
        BAG_FLASH_SIGNAL_PROFILE_STANDARD,
    bag_flash_voicing_flavor flash_voicing_flavor =
        BAG_FLASH_VOICING_FLAVOR_STANDARD);
std::size_t RoundHalfUpFrameScale(int frame_samples, int numerator,
                                  int denominator);
std::size_t ExpectedFlashSampleCount(
    const std::string& text, const test::ConfigCase& config_case,
    bag_flash_signal_profile flash_signal_profile,
    bag_flash_voicing_flavor flash_voicing_flavor);
DecodeResult DecodeViaApi(const bag_decoder_config& config,
                          const bag_pcm16_result& pcm);
DecodeResult DecodeViaApiInChunks(const bag_decoder_config& config,
                                  const bag_pcm16_result& pcm,
                                  std::size_t chunk_sample_count);
void AssertPcmResultsEqual(const bag_pcm16_result& lhs,
                           const bag_pcm16_result& rhs,
                           const std::string& message);
std::string BuildLongEncodeText();
void AssertRoundTripAcrossCorpus(const std::vector<test::CorpusCase>& corpus,
                                 bag_transport_mode mode);

void RegisterApiSyncTests(test::Runner& runner);
void RegisterApiAsyncTests(test::Runner& runner);
void RegisterApiFlashTests(test::Runner& runner);

}  // namespace api_tests
