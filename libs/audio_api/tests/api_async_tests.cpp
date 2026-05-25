#include <array>
#include <string>

#include "api_test_support.h"

namespace {

using namespace api_tests;

static_assert(BAG_ENCODE_OPERATION_QUEUED == 0,
              "Encode operation lifecycle codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_RUNNING == 1,
              "Encode operation lifecycle codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_SUCCEEDED == 2,
              "Encode operation lifecycle codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_FAILED == 3,
              "Encode operation lifecycle codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_CANCELLED == 4,
              "Encode operation lifecycle codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT == 0,
              "Encode operation phase codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM == 1,
              "Encode operation phase codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING == 2,
              "Encode operation phase codes are public ABI.");
static_assert(BAG_ENCODE_OPERATION_PHASE_FINALIZING == 3,
              "Encode operation phase codes are public ABI.");

void TestApiEncodeOperationRejectsInvalidArguments() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config = MakeEncoderConfig(config_case);
  bag_encode_operation* operation = nullptr;
  bag_encode_operation_progress progress{};
  bag_encode_result result{};

  test::AssertEq(
      bag_create_encode_operation(nullptr, "A", &operation),
      BAG_INVALID_ARGUMENT,
      "Null encoder config should be rejected for encode operations.");
  test::AssertEq(
      bag_create_encode_operation(&encoder_config, nullptr, &operation),
      BAG_INVALID_ARGUMENT,
      "Null text should be rejected for encode operations.");
  test::AssertEq(bag_create_encode_operation(&encoder_config, "A", nullptr),
                 BAG_INVALID_ARGUMENT,
                 "Null output operation pointer should be rejected.");

  auto invalid_config = encoder_config;
  invalid_config.sample_rate_hz = 0;
  test::AssertEq(bag_create_encode_operation(&invalid_config, "A", &operation),
                 BAG_INVALID_ARGUMENT,
                 "Invalid configs should not create encode operations.");
  test::AssertTrue(
      operation == nullptr,
      "Invalid encode operation creation should not leave a handle behind.");

  test::AssertEq(bag_run_encode_operation(nullptr), BAG_INVALID_ARGUMENT,
                 "Running a null encode operation should be rejected.");
  test::AssertEq(bag_cancel_encode_operation(nullptr), BAG_INVALID_ARGUMENT,
                 "Cancelling a null encode operation should be rejected.");
  test::AssertEq(bag_poll_encode_operation(nullptr, &progress),
                 BAG_INVALID_ARGUMENT,
                 "Polling a null encode operation should be rejected.");
  test::AssertEq(bag_poll_encode_operation(operation, nullptr),
                 BAG_INVALID_ARGUMENT,
                 "Polling into a null progress output should be rejected.");
  test::AssertEq(
      bag_take_encode_operation_result(nullptr, &result), BAG_INVALID_ARGUMENT,
      "Taking a result from a null encode operation should be rejected.");
  test::AssertEq(
      bag_take_encode_operation_result(operation, nullptr),
      BAG_INVALID_ARGUMENT,
      "Taking a result into a null encode result should be rejected.");
}

void TestApiEncodeOperationPublicContractIsStable() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config =
      MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
  const std::string text = "contract-snapshot";
  bag_encode_operation* operation = nullptr;

  test::AssertEq(
      bag_create_encode_operation(&encoder_config, text.c_str(), &operation),
      BAG_OK,
      "Creating an encode operation should succeed before contract checks.");

  bag_encode_operation_work_plan work_plan{};
  test::AssertEq(bag_get_encode_operation_work_plan(operation, &work_plan),
                 BAG_OK, "Encode operations should expose a public work plan.");

  bag_encode_operation_progress progress{};
  test::AssertEq(bag_poll_encode_operation(operation, &progress), BAG_OK,
                 "Encode operations should expose a public progress snapshot.");

  test::AssertEq(progress.state, BAG_ENCODE_OPERATION_QUEUED,
                 "Fresh encode operations should start queued.");
  test::AssertEq(progress.phase, BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT,
                 "Fresh encode operations should start in preparing-input.");
  test::AssertEq(
      progress.completed_work_units, static_cast<uint64_t>(0),
      "Fresh encode operations should not report completed work yet.");
  test::AssertEq(
      progress.phase_completed_work_units, static_cast<uint64_t>(0),
      "Fresh encode operations should not report completed phase work yet.");
  test::AssertEq(
      progress.total_work_units, work_plan.total_work_units,
      "Progress snapshots should expose the same total work as the work plan.");
  test::AssertEq(progress.estimated_pcm_sample_count,
                 work_plan.estimated_pcm_sample_count,
                 "Progress snapshots should expose the same PCM estimate as "
                 "the work plan.");
  test::AssertEq(progress.payload_byte_count, work_plan.payload_byte_count,
                 "Progress snapshots should expose the same payload size as "
                 "the work plan.");
  test::AssertEq(progress.segment_count, work_plan.segment_count,
                 "Progress snapshots should expose the same segment count as "
                 "the work plan.");
  test::AssertEq(progress.current_segment_index, static_cast<size_t>(0),
                 "Fresh encode operations should start on the first segment.");
  test::AssertEq(progress.phase_total_work_units,
                 work_plan.preparing_input_work_units,
                 "The initial phase total should match the preparing-input "
                 "slice of the work plan.");

  bag_destroy_encode_operation(operation);
}

void TestApiEncodeOperationSuccessAcrossModes() {
  const auto config_case = test::ConfigCases().front();
  const std::string text = "operation-progress";
  const std::array<bag_transport_mode, 3> modes = {
      BAG_TRANSPORT_FLASH,
      BAG_TRANSPORT_PRO,
      BAG_TRANSPORT_ULTRA,
  };

  for (const bag_transport_mode mode : modes) {
    const auto encoder_config = MakeEncoderConfig(config_case, mode);
    bag_encode_operation* operation = nullptr;
    test::AssertEq(
        bag_create_encode_operation(&encoder_config, text.c_str(), &operation),
        BAG_OK,
        "Creating an encode operation should succeed for each transport mode.");
    test::AssertTrue(
        operation != nullptr,
        "Successful encode operation creation should return a handle.");

    bag_encode_operation_progress initial_progress{};
    test::AssertEq(bag_poll_encode_operation(operation, &initial_progress),
                   BAG_OK, "Polling a queued encode operation should succeed.");
    test::AssertEq(initial_progress.state, BAG_ENCODE_OPERATION_QUEUED,
                   "Fresh encode operations should start queued.");
    test::AssertEq(
        initial_progress.terminal_code, BAG_NOT_READY,
        "Fresh encode operations should start in the not-ready terminal code.");
    test::AssertEq(
        initial_progress.payload_byte_count, text.size(),
        "Fresh encode operations should expose the input payload size.");
    test::AssertEq(
        initial_progress.segment_count, static_cast<size_t>(1),
        "Minimal encode operations should expose a single segment by default.");
    test::AssertEq(
        initial_progress.current_segment_index, static_cast<size_t>(0),
        "Minimal encode operations should start on the first segment.");

    bag_encode_result premature_result{};
    test::AssertEq(
        bag_take_encode_operation_result(operation, &premature_result),
        BAG_NOT_READY,
        "Queued encode operations should not expose a result before run.");
    bag_free_encode_result(&premature_result);

    test::AssertEq(bag_run_encode_operation(operation), BAG_OK,
                   "Running an encode operation should succeed.");
    test::AssertEq(
        bag_run_encode_operation(operation), BAG_OK,
        "Running a completed encode operation should stay idempotently OK.");

    bag_encode_operation_progress final_progress{};
    test::AssertEq(bag_poll_encode_operation(operation, &final_progress),
                   BAG_OK,
                   "Polling a completed encode operation should succeed.");
    test::AssertEq(
        final_progress.state, BAG_ENCODE_OPERATION_SUCCEEDED,
        "Completed encode operations should reach the succeeded state.");
    test::AssertEq(
        final_progress.phase, BAG_ENCODE_OPERATION_PHASE_FINALIZING,
        "Successful encode operations should finish in the finalizing phase.");
    test::AssertEq(
        final_progress.terminal_code, BAG_OK,
        "Successful encode operations should publish an OK terminal code.");
    test::AssertEq(
        final_progress.overall_progress_0_to_1, 1.0f,
        "Successful encode operations should finish at 100% overall progress.");
    test::AssertEq(
        final_progress.phase_progress_0_to_1, 1.0f,
        "Successful encode operations should finish at 100% phase progress.");
    test::AssertTrue(final_progress.estimated_pcm_sample_count > 0,
                     "Successful encode operations should publish a non-zero "
                     "PCM sample estimate.");

    bag_encode_result expected_result{};
    bag_encode_result actual_result{};
    bag_encode_result repeated_result{};
    test::AssertEq(bag_encode_text_with_follow(&encoder_config, text.c_str(),
                                               &expected_result),
                   BAG_OK,
                   "The synchronous structured encode baseline should succeed "
                   "for operation comparisons.");
    test::AssertEq(
        bag_take_encode_operation_result(operation, &actual_result), BAG_OK,
        "Succeeded encode operations should expose their structured result.");
    test::AssertEq(
        bag_take_encode_operation_result(operation, &repeated_result), BAG_OK,
        "Succeeded encode operations should allow repeated result retrieval.");
    AssertPcmResultsEqual(
        {expected_result.samples, expected_result.sample_count},
        {actual_result.samples, actual_result.sample_count},
        "Encode operation PCM should match the structured encode baseline.");
    AssertPcmResultsEqual(
        {expected_result.samples, expected_result.sample_count},
        {repeated_result.samples, repeated_result.sample_count},
        "Repeated encode operation result retrieval should stay stable.");
    test::AssertEq(actual_result.raw_payload_available,
                   expected_result.raw_payload_available,
                   "Encode operation raw payload availability should match the "
                   "structured baseline.");
    test::AssertEq(
        actual_result.raw_bytes_hex_size, expected_result.raw_bytes_hex_size,
        "Encode operation raw hex size should be available as a sizing probe.");
    test::AssertEq(actual_result.raw_bytes_hex_status,
                   expected_result.raw_bytes_hex_status,
                   "Encode operation raw hex status should reflect that no "
                   "caller buffer was provided.");
    if (actual_result.raw_bytes_hex_size > 0) {
      test::AssertEq(actual_result.raw_bytes_hex_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty raw hex probes without caller buffers should "
                     "not be readable.");
    }
    test::AssertEq(actual_result.raw_bits_binary_size,
                   expected_result.raw_bits_binary_size,
                   "Encode operation raw bits size should be available as a "
                   "sizing probe.");
    test::AssertEq(actual_result.raw_bits_binary_status,
                   expected_result.raw_bits_binary_status,
                   "Encode operation raw bits status should reflect that no "
                   "caller buffer was provided.");
    if (actual_result.raw_bits_binary_size > 0) {
      test::AssertEq(actual_result.raw_bits_binary_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty raw bit probes without caller buffers should "
                     "not be readable.");
    }
    test::AssertEq(actual_result.follow_data.available,
                   expected_result.follow_data.available,
                   "Encode operation follow availability should match the "
                   "structured baseline.");
    test::AssertEq(actual_result.follow_data.byte_timeline_count,
                   expected_result.follow_data.byte_timeline_count,
                   "Encode operation follow byte timeline count should be "
                   "available as a sizing probe.");
    test::AssertEq(actual_result.follow_data.byte_timeline_status,
                   expected_result.follow_data.byte_timeline_status,
                   "Encode operation byte timeline status should reflect that "
                   "no caller buffer was provided.");
    if (actual_result.follow_data.byte_timeline_count > 0) {
      test::AssertEq(actual_result.follow_data.byte_timeline_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty byte timeline probes without caller buffers "
                     "should not be readable.");
    }
    test::AssertEq(actual_result.follow_data.binary_group_timeline_count,
                   expected_result.follow_data.binary_group_timeline_count,
                   "Encode operation binary-group timeline count should be "
                   "available as a sizing probe.");
    test::AssertEq(actual_result.follow_data.binary_group_timeline_status,
                   expected_result.follow_data.binary_group_timeline_status,
                   "Encode operation binary-group timeline status should "
                   "reflect that no caller buffer was provided.");
    if (actual_result.follow_data.binary_group_timeline_count > 0) {
      test::AssertEq(actual_result.follow_data.binary_group_timeline_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty binary-group probes without caller buffers "
                     "should not be readable.");
    }
    test::AssertEq(actual_result.follow_data.ultra_frame_timeline_count,
                   expected_result.follow_data.ultra_frame_timeline_count,
                   "Encode operation Ultra frame timeline count should be "
                   "available as a sizing probe.");
    test::AssertEq(actual_result.follow_data.ultra_frame_timeline_status,
                   expected_result.follow_data.ultra_frame_timeline_status,
                   "Encode operation Ultra frame status should reflect that no "
                   "caller buffer was provided.");
    if (actual_result.follow_data.ultra_frame_timeline_count > 0) {
      test::AssertEq(actual_result.follow_data.ultra_frame_timeline_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty Ultra frame probes without caller buffers "
                     "should not be readable.");
    }
    test::AssertEq(actual_result.text_follow_data.available,
                   expected_result.text_follow_data.available,
                   "Encode operation text-follow availability should match the "
                   "structured baseline.");
    test::AssertEq(actual_result.text_follow_data.text_token_timeline_count,
                   expected_result.text_follow_data.text_token_timeline_count,
                   "Encode operation text follow token count should be "
                   "available as a sizing probe.");
    test::AssertEq(actual_result.text_follow_data.text_token_timeline_status,
                   expected_result.text_follow_data.text_token_timeline_status,
                   "Encode operation text token status should reflect that no "
                   "caller buffer was provided.");
    if (actual_result.text_follow_data.text_token_timeline_count > 0) {
      test::AssertEq(actual_result.text_follow_data.text_token_timeline_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty text token timeline probes without caller "
                     "buffers should not be readable.");
    }
    test::AssertEq(actual_result.text_follow_data.text_tokens_size,
                   expected_result.text_follow_data.text_tokens_size,
                   "Encode operation serialized text-token size should be "
                   "available as a sizing probe.");
    test::AssertEq(actual_result.text_follow_data.text_tokens_status,
                   expected_result.text_follow_data.text_tokens_status,
                   "Encode operation text-token string status should reflect "
                   "that no caller buffer was provided.");
    if (actual_result.text_follow_data.text_tokens_size > 0) {
      test::AssertEq(actual_result.text_follow_data.text_tokens_status,
                     BAG_DECODE_CONTENT_STATUS_UNAVAILABLE,
                     "Non-empty text-token string probes without caller "
                     "buffers should not be readable.");
    }

    test::AssertEq(
        bag_cancel_encode_operation(operation), BAG_OK,
        "Cancelling a completed encode operation should stay idempotently OK.");

    bag_free_encode_result(&expected_result);
    bag_free_encode_result(&actual_result);
    bag_free_encode_result(&repeated_result);
    bag_destroy_encode_operation(operation);
  }
}

void TestApiEncodeOperationCancelBeforeRun() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config =
      MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
  bag_encode_operation* operation = nullptr;

  test::AssertEq(
      bag_create_encode_operation(&encoder_config,
                                  BuildLongEncodeText().c_str(), &operation),
      BAG_OK,
      "Creating a long flash encode operation should succeed before cancel.");
  test::AssertEq(bag_cancel_encode_operation(operation), BAG_OK,
                 "Cancelling a queued encode operation should succeed.");
  test::AssertEq(bag_cancel_encode_operation(operation), BAG_OK,
                 "Repeated queued cancel should stay idempotently OK.");

  bag_encode_operation_progress progress{};
  test::AssertEq(bag_poll_encode_operation(operation, &progress), BAG_OK,
                 "Polling a cancelled queued encode operation should succeed.");
  test::AssertEq(progress.state, BAG_ENCODE_OPERATION_CANCELLED,
                 "Cancelled-before-run encode operations should report the "
                 "cancelled state.");
  test::AssertEq(progress.terminal_code, BAG_CANCELLED,
                 "Cancelled-before-run encode operations should publish the "
                 "cancelled terminal code.");
  test::AssertEq(progress.overall_progress_0_to_1, 0.0f,
                 "Cancelled-before-run encode operations should not advance "
                 "overall progress.");

  bag_encode_result result{};
  test::AssertEq(bag_take_encode_operation_result(operation, &result),
                 BAG_CANCELLED,
                 "Cancelled-before-run encode operations should not expose a "
                 "structured result.");
  test::AssertTrue(
      result.samples == nullptr,
      "Cancelled-before-run encode operations should not allocate PCM.");
  test::AssertEq(
      result.sample_count, static_cast<size_t>(0),
      "Cancelled-before-run encode operations should report no PCM samples.");

  test::AssertEq(bag_run_encode_operation(operation), BAG_CANCELLED,
                 "Running a cancelled-before-run encode operation should "
                 "return cancelled.");

  bag_free_encode_result(&result);
  bag_destroy_encode_operation(operation);
}

void TestApiMiniWhitespaceEncodeOperationSuccess() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config =
      MakeEncoderConfig(config_case, BAG_TRANSPORT_MINI);
  const std::array<std::string, 3> cases = {
      "   ",
      "\n",
      " \t\r\n ",
  };

  for (const auto& text : cases) {
    bag_encode_operation* operation = nullptr;
    test::AssertEq(
        bag_create_encode_operation(&encoder_config, text.c_str(), &operation),
        BAG_OK,
        "Mini encode operations should accept whitespace-only text because "
        "separators are encodable.");

    test::AssertEq(
        bag_run_encode_operation(operation), BAG_OK,
        "Whitespace-only mini encode operations should run successfully.");

    bag_encode_operation_progress progress{};
    test::AssertEq(bag_poll_encode_operation(operation, &progress), BAG_OK,
                   "Whitespace-only mini encode operations should expose "
                   "terminal progress.");
    test::AssertEq(progress.state, BAG_ENCODE_OPERATION_SUCCEEDED,
                   "Whitespace-only mini encode operations should reach the "
                   "succeeded terminal state.");
    test::AssertEq(progress.terminal_code, BAG_OK,
                   "Whitespace-only mini encode operations should publish an "
                   "OK terminal code.");

    bag_encode_result result{};
    test::AssertEq(
        bag_take_encode_operation_result(operation, &result), BAG_OK,
        "Whitespace-only mini encode operations should expose a PCM result.");
    test::AssertTrue(result.sample_count > 0,
                     "Whitespace-only mini encode operations should render "
                     "non-empty silence PCM.");

    bag_free_encode_result(&result);
    bag_destroy_encode_operation(operation);
  }
}

void TestApiEncodeOperationPumpAndWorkPlan() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config =
      MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
  const std::string text = "pump-progress-check";
  bag_encode_operation* operation = nullptr;

  test::AssertEq(
      bag_create_encode_operation(&encoder_config, text.c_str(), &operation),
      BAG_OK,
      "Creating an encode operation should succeed before work-plan "
      "inspection.");

  bag_encode_operation_work_plan work_plan{};
  test::AssertEq(bag_get_encode_operation_work_plan(operation, &work_plan),
                 BAG_OK, "Encode operations should expose a work plan.");
  test::AssertTrue(
      work_plan.total_work_units > 0,
      "Encode operation work plans should expose non-zero total work.");
  test::AssertTrue(
      work_plan.preparing_input_work_units > 0,
      "Encode operation work plans should expose preparing-input work.");
  test::AssertTrue(work_plan.rendering_pcm_work_units > 0,
                   "Encode operation work plans should expose rendering work.");
  test::AssertEq(
      work_plan.payload_byte_count, text.size(),
      "Encode operation work plans should expose payload byte count.");

  bag_encode_operation_progress initial_progress{};
  test::AssertEq(bag_poll_encode_operation(operation, &initial_progress),
                 BAG_OK, "Polling before pump should succeed.");
  test::AssertEq(initial_progress.total_work_units, work_plan.total_work_units,
                 "Snapshot total work should match the work plan.");

  bool saw_progress = false;
  for (int attempt = 0; attempt < 2000; ++attempt) {
    int did_progress = 0;
    test::AssertEq(
        bag_pump_encode_operation(operation, {4096, 5}, &did_progress), BAG_OK,
        "Pumping an encode operation should succeed.");
    if (did_progress != 0) {
      saw_progress = true;
    }

    bag_encode_operation_progress progress{};
    test::AssertEq(bag_poll_encode_operation(operation, &progress), BAG_OK,
                   "Polling after pump should succeed.");
    test::AssertEq(
        bag_get_encode_operation_work_plan(operation, &work_plan), BAG_OK,
        "Encode operations should keep exposing a work plan after pump.");
    test::AssertEq(progress.total_work_units, work_plan.total_work_units,
                   "Progress snapshots should keep total work aligned with the "
                   "current work plan.");
    test::AssertTrue(progress.completed_work_units <= progress.total_work_units,
                     "Completed work should never exceed total work.");
    test::AssertTrue(
        progress.phase_completed_work_units <= progress.phase_total_work_units,
        "Phase-completed work should never exceed phase total work.");

    if (progress.state == BAG_ENCODE_OPERATION_SUCCEEDED) {
      test::AssertTrue(
          saw_progress ||
              progress.completed_work_units == progress.total_work_units,
          "The pump path should either observe progress or finish completely.");
      test::AssertEq(
          progress.completed_work_units, progress.total_work_units,
          "Successful operations should complete all planned work units.");
      break;
    }

    if (attempt == 1999) {
      test::Fail("Pumped encode operation did not finish before timeout.");
    }
  }

  bag_destroy_encode_operation(operation);
}

void TestApiEncodeOperationSmallPumpAdvancesIncrementally() {
  const auto config_case = test::ConfigCases().front();
  const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
  const std::string text = "A";
  bag_encode_operation* operation = nullptr;

  test::AssertEq(
      bag_create_encode_operation(&encoder_config, text.c_str(), &operation),
      BAG_OK,
      "Creating a pro encode operation should succeed for incremental pump "
      "tests.");

  bag_encode_operation_progress progress{};
  std::uint64_t previous_completed_units = 0;
  bool observed_partial_progress = false;
  for (int attempt = 0; attempt < 256; ++attempt) {
    int did_progress = 0;
    test::AssertEq(bag_pump_encode_operation(operation, {1, 0}, &did_progress),
                   BAG_OK, "Small-budget pump should succeed.");
    test::AssertEq(bag_poll_encode_operation(operation, &progress), BAG_OK,
                   "Polling after a small-budget pump should succeed.");
    test::AssertTrue(progress.completed_work_units >= previous_completed_units,
                     "Completed work units should stay monotonic under "
                     "repeated small pumps.");
    if (progress.completed_work_units > previous_completed_units &&
        progress.completed_work_units < progress.total_work_units) {
      observed_partial_progress = true;
    }
    previous_completed_units = progress.completed_work_units;
    if (progress.state == BAG_ENCODE_OPERATION_SUCCEEDED) {
      break;
    }
  }

  test::AssertTrue(observed_partial_progress,
                   "Small-budget pumps should expose an intermediate "
                   "partially-complete state.");
  test::AssertEq(
      progress.state, BAG_ENCODE_OPERATION_SUCCEEDED,
      "Repeated small-budget pumps should eventually finish the operation.");
  bag_destroy_encode_operation(operation);
}

}  // namespace

namespace api_tests {

void RegisterApiAsyncTests(test::Runner& runner) {
  runner.Add("Api.EncodeOperationRejectsInvalidArguments",
             TestApiEncodeOperationRejectsInvalidArguments);
  runner.Add("Api.EncodeOperationPublicContractIsStable",
             TestApiEncodeOperationPublicContractIsStable);
  runner.Add("Api.EncodeOperationSuccessAcrossModes",
             TestApiEncodeOperationSuccessAcrossModes);
  runner.Add("Api.EncodeOperationCancelBeforeRun",
             TestApiEncodeOperationCancelBeforeRun);
  runner.Add("Api.MiniWhitespaceEncodeOperationSuccess",
             TestApiMiniWhitespaceEncodeOperationSuccess);
  runner.Add("Api.EncodeOperationPumpAndWorkPlan",
             TestApiEncodeOperationPumpAndWorkPlan);
  runner.Add("Api.EncodeOperationSmallPumpAdvancesIncrementally",
             TestApiEncodeOperationSmallPumpAdvancesIncrementally);
}

}  // namespace api_tests
