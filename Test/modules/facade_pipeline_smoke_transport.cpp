#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.common.config;
import bag.common.error_code;
import bag.pipeline;
import bag.transport.facade;

#include "facade_pipeline_smoke_support.h"

namespace {

using namespace modules_facade_pipeline_smoke;

void PushAndPollExpectingText(std::unique_ptr<bag::ITransportDecoder> decoder,
                              const std::vector<std::int16_t>& pcm,
                              bag::TransportMode mode,
                              std::string_view text) {
    test::AssertTrue(decoder != nullptr, "Transport facade should return a decoder instance.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 1234;
    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Transport facade decoder should accept PCM input.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Transport facade decoder should decode a result.");
    test::AssertEq(result.text, std::string(text), "Transport facade decoder should preserve decoded text.");
    test::AssertEq(result.mode, mode, "Transport facade decoder should preserve the transport mode.");
}

void TestTransportFacadeValidation() {
    auto config = MakeConfig(bag::TransportMode::kPro);
    const auto non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, non_ascii),
        bag::TransportValidationIssue::kProAsciiOnly,
        "Transport facade module should keep the pro ASCII-only validation rule.");

    config.sample_rate_hz = 0;
    test::AssertEq(
        bag::ValidateDecoderConfig(config),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport facade module should validate decoder sample rate.");
}

void TestTransportFacadeRoundTrip(bag::TransportMode mode, std::string_view text) {
    const auto config = MakeConfig(mode);

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(config, std::string(text), &pcm),
        bag::ErrorCode::kOk,
        "Transport facade module should encode text for the requested mode.");
    test::AssertTrue(!pcm.empty(), "Transport facade module should emit PCM.");

    PushAndPollExpectingText(bag::CreateTransportDecoder(config), pcm, mode, text);
}

void TestTransportFacadeFlashRoundTrip() {
    TestTransportFacadeRoundTrip(
        bag::TransportMode::kFlash,
        test::Utf8Literal(u8"Phase4-Flash"));
}

void TestTransportFacadeProRoundTrip() {
    TestTransportFacadeRoundTrip(bag::TransportMode::kPro, "Phase4-Pro");
}

void TestTransportFacadeUltraRoundTrip() {
    TestTransportFacadeRoundTrip(
        bag::TransportMode::kUltra,
        test::Utf8Literal(u8"Phase4-Ultra-超级"));
}

void TestTransportFacadeChunkedRoundTrip() {
    struct Case {
        bag::TransportMode mode;
        std::string text;
    };
    const std::array<Case, 3> cases = {{
        {bag::TransportMode::kFlash, test::Utf8Literal(u8"Facade-Flash-你好")},
        {bag::TransportMode::kPro, "Facade-Pro-123"},
        {bag::TransportMode::kUltra, test::Utf8Literal(u8"Facade-Ultra-超级")},
    }};

    for (const auto& item : cases) {
        const auto config = MakeConfig(item.mode);
        std::vector<std::int16_t> pcm;
        test::AssertEq(
            bag::EncodeTextToPcm16(config, item.text, &pcm),
            bag::ErrorCode::kOk,
            "Chunked facade encode should succeed.");

        auto decoder = bag::CreateTransportDecoder(config);
        PushPcmInChunks(decoder.get(), pcm, std::max<std::size_t>(1, pcm.size() / static_cast<std::size_t>(6)),
                        "Transport facade");

        bag::TextResult result{};
        test::AssertEq(
            decoder->PollTextResult(&result),
            bag::ErrorCode::kOk,
            "Chunked transport facade poll should succeed.");
        test::AssertEq(result.text, item.text, "Chunked transport facade should preserve decoded text.");
        test::AssertEq(result.mode, item.mode, "Chunked transport facade should preserve transport mode.");
    }
}

struct EncodeOperationCase {
    bag::TransportMode mode;
    std::string text;
};

void TestEncodeOperationRoundTripsAllModes() {
    const std::array<EncodeOperationCase, 4> cases = {{
        {bag::TransportMode::kMini, "sos 2"},
        {bag::TransportMode::kFlash, test::Utf8Literal(u8"Operation-闪")},
        {bag::TransportMode::kPro, "Operation-Pro-123"},
        {bag::TransportMode::kUltra,
         test::Utf8Literal(u8"Operation-Ultra-超级")},
    }};

    for (const auto& item : cases) {
        const auto config = MakeConfig(item.mode);
        auto operation = bag::CreateEncodeOperation(config, item.text);
        test::AssertTrue(operation != nullptr,
                         "Encode operation should be created for a valid mode.");

        const bag::EncodeWorkPlan work_plan = operation->WorkPlan();
        test::AssertTrue(work_plan.total_work_units > 0,
                         "Encode operation should expose positive work.");
        const bag::EncodeProgressSnapshot queued = operation->Snapshot();
        test::AssertEq(queued.state, bag::EncodeOperationState::kQueued,
                       "New encode operation should start queued.");
        test::AssertEq(operation->TakeResult(nullptr),
                       bag::ErrorCode::kInvalidArgument,
                       "TakeResult should reject a null output before running.");

        std::uint64_t previous_completed = 0;
        bool saw_progress = false;
        bool did_progress = false;
        for (int pump_count = 0; pump_count < 2000; ++pump_count) {
            test::AssertEq(
                operation->Pump({256, 0}, &did_progress), bag::ErrorCode::kOk,
                "Bounded encode operation pump should return success.");
            const bag::EncodeProgressSnapshot snapshot = operation->Snapshot();
            test::AssertTrue(snapshot.completed_work_units >= previous_completed,
                             "Encode operation progress should be monotonic.");
            test::AssertTrue(snapshot.overall_progress_0_to_1 >= 0.0f &&
                                 snapshot.overall_progress_0_to_1 <= 1.0f,
                             "Encode operation overall progress should stay bounded.");
            if (did_progress) {
                saw_progress = true;
            }
            previous_completed = snapshot.completed_work_units;
            if (snapshot.state == bag::EncodeOperationState::kSucceeded) {
                break;
            }
            test::AssertTrue(snapshot.state != bag::EncodeOperationState::kFailed,
                             "Valid encode operation should not fail while pumping.");
        }

        const bag::EncodeProgressSnapshot completed = operation->Snapshot();
        test::AssertTrue(saw_progress,
                         "Bounded encode operation should report progress.");
        test::AssertEq(completed.state, bag::EncodeOperationState::kSucceeded,
                       "Encode operation should reach succeeded state.");
        test::AssertEq(completed.terminal_code, bag::ErrorCode::kOk,
                       "Completed encode operation should report ok terminal code.");
        test::AssertEq(completed.completed_work_units,
                       completed.total_work_units,
                       "Completed encode operation should finish all work.");
        test::AssertEq(completed.overall_progress_0_to_1, 1.0f,
                       "Completed encode operation should report full progress.");

        bag::EncodedPcmFollowResult result{};
        test::AssertEq(operation->TakeResult(&result), bag::ErrorCode::kOk,
                       "Completed encode operation should expose its result.");
        test::AssertTrue(!result.pcm.empty(),
                         "Completed encode operation should return PCM.");
        test::AssertTrue(result.follow_data.available,
                         "Completed encode operation should return payload follow data.");
        test::AssertTrue(result.text_follow_data.available,
                         "Completed encode operation should return text follow data.");

        std::size_t previous_byte_end = 0;
        for (std::size_t index = 0; index < result.follow_data.byte_timeline.size();
             ++index) {
            const auto& entry = result.follow_data.byte_timeline[index];
            test::AssertEq(entry.byte_index, index,
                           "Follow byte timeline should preserve payload order.");
            test::AssertTrue(entry.sample_count > 0,
                             "Follow byte timeline entries should own samples.");
            test::AssertTrue(entry.start_sample >= previous_byte_end,
                             "Follow byte timeline should not overlap bytes.");
            previous_byte_end = entry.start_sample + entry.sample_count;
        }

        std::size_t previous_group_end = 0;
        for (std::size_t index = 0;
             index < result.follow_data.binary_group_timeline.size(); ++index) {
            const auto& entry = result.follow_data.binary_group_timeline[index];
            test::AssertEq(entry.group_index, index,
                           "Follow binary groups should preserve order.");
            test::AssertTrue(entry.sample_count > 0,
                             "Follow binary groups should own samples.");
            test::AssertTrue(entry.start_sample >= previous_group_end,
                             "Follow binary groups should not overlap.");
            previous_group_end = entry.start_sample + entry.sample_count;
        }

        switch (item.mode) {
            case bag::TransportMode::kMini:
                test::AssertEq(result.follow_data.payload_begin_sample,
                               static_cast<std::size_t>(0),
                               "Mini follow payload should start at sample zero.");
                for (const auto& entry : result.follow_data.binary_group_timeline) {
                    test::AssertEq(entry.bit_count, static_cast<std::size_t>(1),
                                   "Mini follow groups should represent one Morse element.");
                }
                break;
            case bag::TransportMode::kFlash:
                test::AssertEq(result.follow_data.byte_timeline.size(),
                               result.follow_data.raw_payload_bytes.size(),
                               "Flash follow should publish one timeline entry per byte.");
                for (const auto& entry : result.follow_data.binary_group_timeline) {
                    test::AssertEq(entry.bit_count, static_cast<std::size_t>(1),
                                   "Flash follow groups should represent one bit.");
                    test::AssertTrue(
                        entry.bit_offset <
                            result.follow_data.raw_payload_bytes.size() * 8,
                        "Flash follow bit offsets should stay within the payload.");
                }
                break;
            case bag::TransportMode::kPro:
                test::AssertEq(
                    result.follow_data.binary_group_timeline.size(),
                    result.follow_data.raw_payload_bytes.size() * 2,
                    "Pro follow should publish two nibble groups per byte.");
                for (const auto& entry : result.follow_data.binary_group_timeline) {
                    test::AssertEq(entry.bit_count, static_cast<std::size_t>(4),
                                   "Pro follow groups should represent one nibble.");
                }
                break;
            case bag::TransportMode::kUltra:
                test::AssertEq(
                    result.follow_data.ultra_frame_timeline.size(),
                    result.follow_data.binary_group_timeline.size(),
                    "Ultra follow should mirror frame symbols in binary groups.");
                test::AssertTrue(
                    result.follow_data.ultra_frame_timeline.size() > 12,
                    "Ultra follow should include preamble, payload, and tail symbols.");
                test::AssertEq(result.follow_data.ultra_frame_timeline.front().section,
                               bag::UltraFrameSection::kPreamble,
                               "Ultra follow should begin with a preamble.");
                test::AssertEq(result.follow_data.ultra_frame_timeline[8].section,
                               bag::UltraFrameSection::kPayload,
                               "Ultra follow should mark symbols after preamble as payload.");
                test::AssertTrue(
                    result.follow_data.ultra_frame_timeline[8].is_payload,
                    "Ultra follow payload symbols should be marked active.");
                const auto tail_begin =
                    result.follow_data.ultra_frame_timeline.size() - 4;
                test::AssertEq(
                    result.follow_data.ultra_frame_timeline[tail_begin].section,
                    bag::UltraFrameSection::kTail,
                    "Ultra follow should end with a tail.");
                test::AssertEq(
                    result.follow_data.payload_begin_sample,
                    result.follow_data.ultra_frame_timeline[8].start_sample,
                    "Ultra payload should begin after the preamble.");
                break;
        }

        bag::EncodedPcmFollowResult repeated{};
        test::AssertEq(operation->TakeResult(&repeated), bag::ErrorCode::kOk,
                       "Taking a completed result should be repeatable.");
        test::AssertEq(repeated.pcm, result.pcm,
                       "Repeated result reads should preserve PCM.");
    }
}

void TestEncodeOperationCancellationLifecycle() {
    const auto config = MakeConfig(bag::TransportMode::kUltra);
    const std::string text = test::Utf8Literal(u8"cancel-operation-长文本");

    auto queued_operation = bag::CreateEncodeOperation(config, text);
    test::AssertTrue(queued_operation != nullptr,
                     "Queued cancellation fixture should be created.");
    test::AssertEq(queued_operation->Cancel(), bag::ErrorCode::kOk,
                   "Queued encode operation cancellation should succeed.");
    test::AssertEq(queued_operation->Snapshot().state,
                   bag::EncodeOperationState::kCancelled,
                   "Queued cancellation should enter cancelled state.");
    bag::EncodedPcmFollowResult queued_result{};
    test::AssertEq(queued_operation->TakeResult(&queued_result),
                   bag::ErrorCode::kCancelled,
                   "Cancelled queued operation should not expose a result.");

    auto running_operation = bag::CreateEncodeOperation(config, text);
    test::AssertTrue(running_operation != nullptr,
                     "Running cancellation fixture should be created.");
    bool did_progress = false;
    test::AssertEq(running_operation->Pump({1, 0}, &did_progress),
                   bag::ErrorCode::kOk,
                   "Cancellation fixture should accept an initial bounded pump.");
    test::AssertEq(running_operation->Cancel(), bag::ErrorCode::kOk,
                   "Running encode operation cancellation should succeed.");
    test::AssertEq(running_operation->Snapshot().state,
                   bag::EncodeOperationState::kRunning,
                   "Running cancellation should remain running until pumped.");
    did_progress = true;
    test::AssertEq(running_operation->Pump({256, 0}, &did_progress),
                   bag::ErrorCode::kOk,
                   "Pumping a cancelled operation should be harmless.");
    test::AssertTrue(!did_progress,
                     "Pumping a cancelled operation should not report progress.");
    test::AssertEq(running_operation->Snapshot().state,
                   bag::EncodeOperationState::kCancelled,
                   "Pumping a cancellation request should enter cancelled state.");
}

void TestEncodeOperationRejectsInvalidRequests() {
    auto invalid_mode = MakeConfig(bag::TransportMode::kFlash);
    invalid_mode.mode = static_cast<bag::TransportMode>(255);
    test::AssertTrue(
        bag::CreateEncodeOperation(invalid_mode, "invalid") == nullptr,
        "Encode operation should reject an invalid transport mode.");

    const auto config = MakeConfig(bag::TransportMode::kMini);
    test::AssertTrue(bag::CreateEncodeOperation(config, "") == nullptr,
                     "Encode operation should reject empty text.");

    auto operation = bag::CreateEncodeOperation(config, "valid");
    test::AssertTrue(operation != nullptr,
                     "Valid operation fixture should be created.");
    test::AssertEq(operation->TakeResult(nullptr),
                   bag::ErrorCode::kInvalidArgument,
                   "TakeResult should consistently reject a null output.");
    bag::EncodedPcmFollowResult result{};
    test::AssertEq(operation->TakeResult(&result), bag::ErrorCode::kNotReady,
                   "TakeResult before completion should report not ready.");
}

}  // namespace

namespace modules_facade_pipeline_smoke {

void RegisterTransportFacadeSmokeTests(test::Runner& runner) {
    runner.Add("ModulesFacadePipeline.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("ModulesFacadePipeline.TransportFacadeFlashRoundTrip", TestTransportFacadeFlashRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeProRoundTrip", TestTransportFacadeProRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeUltraRoundTrip", TestTransportFacadeUltraRoundTrip);
    runner.Add("ModulesFacadePipeline.TransportFacadeChunkedRoundTrip", TestTransportFacadeChunkedRoundTrip);
    runner.Add("ModulesFacadePipeline.EncodeOperationRoundTripsAllModes",
               TestEncodeOperationRoundTripsAllModes);
    runner.Add("ModulesFacadePipeline.EncodeOperationCancellationLifecycle",
               TestEncodeOperationCancellationLifecycle);
    runner.Add("ModulesFacadePipeline.EncodeOperationRejectsInvalidRequests",
               TestEncodeOperationRejectsInvalidRequests);
}

}  // namespace modules_facade_pipeline_smoke
