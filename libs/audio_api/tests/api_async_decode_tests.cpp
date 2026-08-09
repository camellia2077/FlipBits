#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include "api_test_support.h"

namespace {

using namespace api_tests;

static_assert(BAG_DECODE_OPERATION_QUEUED == 0,
              "Decode operation lifecycle codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_RUNNING == 1,
              "Decode operation lifecycle codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_SUCCEEDED == 2,
              "Decode operation lifecycle codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_FAILED == 3,
              "Decode operation lifecycle codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_CANCELLED == 4,
              "Decode operation lifecycle codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT == 0,
              "Decode operation phase codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_PHASE_READING_PCM == 1,
              "Decode operation phase codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD == 2,
              "Decode operation phase codes are public ABI.");
static_assert(BAG_DECODE_OPERATION_PHASE_FINALIZING == 3,
              "Decode operation phase codes are public ABI.");

void TestApiDecodeOperationRejectsInvalidArguments() {
  const auto config_case = test::ConfigCases().front();
  const auto decoder_config = MakeDecoderConfig(config_case);
  std::array<int16_t, 4> pcm{};
  bag_decode_operation* operation = nullptr;
  bag_decode_operation_progress progress{};
  bag_decode_result result{};

  test::AssertEq(
      bag_create_decode_operation(nullptr, pcm.data(), pcm.size(), &operation),
      BAG_INVALID_ARGUMENT,
      "Null decoder config should be rejected for decode operations.");
  test::AssertEq(
      bag_create_decode_operation(&decoder_config, nullptr, pcm.size(),
                                  &operation),
      BAG_INVALID_ARGUMENT,
      "Null PCM should be rejected for decode operations.");
  test::AssertEq(
      bag_create_decode_operation(&decoder_config, pcm.data(), 0, &operation),
      BAG_INVALID_ARGUMENT,
      "Empty PCM should be rejected for decode operations.");
  test::AssertEq(
      bag_create_decode_operation(&decoder_config, pcm.data(), pcm.size(),
                                  nullptr),
      BAG_INVALID_ARGUMENT,
      "Null output operation pointer should be rejected.");

  auto invalid_config = decoder_config;
  invalid_config.sample_rate_hz = 0;
  test::AssertEq(bag_create_decode_operation(&invalid_config, pcm.data(),
                                             pcm.size(), &operation),
                 BAG_INVALID_ARGUMENT,
                 "Invalid configs should not create decode operations.");
  test::AssertTrue(
      operation == nullptr,
      "Invalid decode operation creation should not leave a handle behind.");

  test::AssertEq(bag_run_decode_operation(nullptr), BAG_INVALID_ARGUMENT,
                 "Running a null decode operation should be rejected.");
  test::AssertEq(bag_cancel_decode_operation(nullptr), BAG_INVALID_ARGUMENT,
                 "Cancelling a null decode operation should be rejected.");
  test::AssertEq(bag_poll_decode_operation(nullptr, &progress),
                 BAG_INVALID_ARGUMENT,
                 "Polling a null decode operation should be rejected.");
  test::AssertEq(bag_poll_decode_operation(operation, nullptr),
                 BAG_INVALID_ARGUMENT,
                 "Polling into a null progress output should be rejected.");
  test::AssertEq(
      bag_take_decode_operation_result(nullptr, &result), BAG_INVALID_ARGUMENT,
      "Taking a result from a null decode operation should be rejected.");
  test::AssertEq(
      bag_take_decode_operation_result(operation, nullptr),
      BAG_INVALID_ARGUMENT,
      "Taking a result into a null decode result should be rejected.");
}

void TestApiDecodeOperationPumpAndResult() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
  const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
  const std::string text = "decode-operation-progress";
  bag_pcm16_result pcm{};
  test::AssertEq(bag_encode_text(&encoder_config, text.c_str(), &pcm), BAG_OK,
                 "C API encode should produce PCM for decode operation tests.");

  bag_decode_operation* operation = nullptr;
  test::AssertEq(bag_create_decode_operation(&decoder_config, pcm.samples,
                                             pcm.sample_count, &operation),
                 BAG_OK,
                 "Creating a decode operation should succeed for encoded PCM.");
  test::AssertTrue(operation != nullptr,
                   "Successful decode operation creation should return a "
                   "handle.");

  bag_decode_operation_work_plan work_plan{};
  test::AssertEq(bag_get_decode_operation_work_plan(operation, &work_plan),
                 BAG_OK, "Decode operations should expose a public work plan.");
  test::AssertTrue(work_plan.total_work_units > 0,
                   "Decode operation work plans should expose non-zero work.");
  test::AssertEq(work_plan.pcm_sample_count, pcm.sample_count,
                 "Decode operation work plans should expose PCM sample count.");
  test::AssertEq(work_plan.reading_pcm_work_units,
                 static_cast<uint64_t>(pcm.sample_count),
                 "Reading work should be based on PCM samples.");

  bag_decode_operation_progress initial_progress{};
  test::AssertEq(bag_poll_decode_operation(operation, &initial_progress),
                 BAG_OK, "Polling a queued decode operation should succeed.");
  test::AssertEq(initial_progress.state, BAG_DECODE_OPERATION_QUEUED,
                 "Fresh decode operations should start queued.");
  test::AssertEq(initial_progress.phase,
                 BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT,
                 "Fresh decode operations should start in preparing-input.");
  test::AssertEq(initial_progress.terminal_code, BAG_NOT_READY,
                 "Fresh decode operations should start not-ready.");
  test::AssertEq(initial_progress.total_work_units, work_plan.total_work_units,
                 "Decode progress total work should match the work plan.");

  bag_decode_result premature_result{};
  test::AssertEq(
      bag_take_decode_operation_result(operation, &premature_result),
      BAG_NOT_READY,
      "Queued decode operations should not expose a result before pump.");

  bool observed_partial_progress = false;
  std::uint64_t previous_completed_units = 0;
  for (int attempt = 0; attempt < 10000; ++attempt) {
    int did_progress = 0;
    test::AssertEq(bag_pump_decode_operation(operation, {128, 0},
                                             &did_progress),
                   BAG_OK, "Pumping a decode operation should succeed.");
    bag_decode_operation_progress progress{};
    test::AssertEq(bag_poll_decode_operation(operation, &progress), BAG_OK,
                   "Polling after decode pump should succeed.");
    test::AssertTrue(progress.completed_work_units >= previous_completed_units,
                     "Decode completed work should stay monotonic.");
    test::AssertTrue(progress.completed_work_units <= progress.total_work_units,
                     "Decode completed work should never exceed total work.");
    test::AssertTrue(progress.pushed_pcm_sample_count <= pcm.sample_count,
                     "Decode pushed PCM should never exceed source PCM.");
    if (progress.completed_work_units > previous_completed_units &&
        progress.completed_work_units < progress.total_work_units) {
      observed_partial_progress = true;
    }
    previous_completed_units = progress.completed_work_units;
    if (progress.state == BAG_DECODE_OPERATION_SUCCEEDED) {
      test::AssertEq(progress.terminal_code, BAG_OK,
                     "Successful decode operations should publish OK.");
      test::AssertEq(progress.overall_progress_0_to_1, 1.0f,
                     "Successful decode operations should finish at 100%.");
      break;
    }
    if (attempt == 9999) {
      test::Fail("Pumped decode operation did not finish before timeout.");
    }
  }
  test::AssertTrue(observed_partial_progress,
                   "Small-budget decode pumps should expose intermediate "
                   "progress.");

  std::vector<char> text_buffer(4096, '\0');
  bag_decode_result result{};
  result.text_buffer = text_buffer.data();
  result.text_buffer_size = text_buffer.size();
  test::AssertEq(bag_take_decode_operation_result(operation, &result), BAG_OK,
                 "Succeeded decode operations should expose a result.");
  test::AssertEq(std::string(text_buffer.data(), result.text_size), text,
                 "Decode operation result should match the encoded text.");
  test::AssertEq(result.mode, BAG_TRANSPORT_PRO,
                 "Decode operation result mode should match the config.");

  bag_destroy_decode_operation(operation);
  bag_free_pcm16_result(&pcm);
}

void TestApiDecodeOperationCancelBeforeRun() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config =
      MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
  const auto decoder_config =
      MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
  bag_pcm16_result pcm{};
  test::AssertEq(bag_encode_text(&encoder_config, "cancel-decode", &pcm),
                 BAG_OK,
                 "C API encode should produce PCM for decode cancel tests.");

  bag_decode_operation* operation = nullptr;
  test::AssertEq(bag_create_decode_operation(&decoder_config, pcm.samples,
                                             pcm.sample_count, &operation),
                 BAG_OK,
                 "Creating a decode operation should succeed before cancel.");
  test::AssertEq(bag_cancel_decode_operation(operation), BAG_OK,
                 "Cancelling a queued decode operation should succeed.");
  test::AssertEq(bag_cancel_decode_operation(operation), BAG_OK,
                 "Repeated queued decode cancel should stay OK.");

  bag_decode_operation_progress progress{};
  test::AssertEq(bag_poll_decode_operation(operation, &progress), BAG_OK,
                 "Polling a cancelled decode operation should succeed.");
  test::AssertEq(progress.state, BAG_DECODE_OPERATION_CANCELLED,
                 "Cancelled decode operations should report cancelled.");
  test::AssertEq(progress.terminal_code, BAG_CANCELLED,
                 "Cancelled decode operations should publish cancelled.");

  bag_decode_result result{};
  test::AssertEq(bag_take_decode_operation_result(operation, &result),
                 BAG_CANCELLED,
                 "Cancelled decode operations should not expose a result.");
  test::AssertEq(bag_run_decode_operation(operation), BAG_CANCELLED,
                 "Running a cancelled decode operation should return "
                 "cancelled.");

  bag_destroy_decode_operation(operation);
  bag_free_pcm16_result(&pcm);
}

}  // namespace

namespace api_tests {

void RegisterApiAsyncDecodeTests(test::Runner& runner) {
  runner.Add("Api.DecodeOperationRejectsInvalidArguments",
             TestApiDecodeOperationRejectsInvalidArguments);
  runner.Add("Api.DecodeOperationPumpAndResult",
             TestApiDecodeOperationPumpAndResult);
  runner.Add("Api.DecodeOperationCancelBeforeRun",
             TestApiDecodeOperationCancelBeforeRun);
}

}  // namespace api_tests
