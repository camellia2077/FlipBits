#include <algorithm>
#include <array>
#include <chrono>
#include <string>
#include <thread>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_utf8.h"
#include "test_vectors.h"

namespace {

struct DecodeResult {
    bag_error_code code = BAG_INTERNAL;
    std::string text;
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

struct EncodeJobCompletion {
    bag_encode_job_progress final_progress{};
    bool saw_running = false;
    bool saw_postprocessing = false;
    bool saw_phase_regression = false;
    int progress_advance_count = 0;
};

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

std::size_t RoundHalfUpFrameScale(int frame_samples, int numerator, int denominator) {
    return frame_samples > 0
               ? static_cast<std::size_t>((frame_samples * numerator + (denominator / 2)) / denominator)
               : static_cast<std::size_t>(0);
}

std::size_t ExpectedFlashSampleCount(const std::string& text,
                                     const test::ConfigCase& config_case,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    const std::size_t frame_samples =
        config_case.frame_samples > 0 ? static_cast<std::size_t>(config_case.frame_samples) : static_cast<std::size_t>(0);
    const std::size_t payload_samples_per_bit =
        flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT
            ? RoundHalfUpFrameScale(config_case.frame_samples, 3, 1)
            : frame_samples;
    const std::size_t leading_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(16)
            : frame_samples * static_cast<std::size_t>(3);
    const std::size_t trailing_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(8)
            : frame_samples * static_cast<std::size_t>(3);
    return text.size() * static_cast<std::size_t>(8) * payload_samples_per_bit +
           leading_nonpayload_samples +
           trailing_nonpayload_samples;
}

DecodeResult DecodeViaApi(const bag_decoder_config& config, const bag_pcm16_result& pcm) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Decoder should not be null after creation.");

    const auto push_code = bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0);
    test::AssertEq(push_code, BAG_OK, "PCM push should succeed.");

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.mode = result.mode;
    return out;
}

bool IsEncodeJobTerminal(bag_encode_job_state state) {
    return state == BAG_ENCODE_JOB_SUCCEEDED ||
           state == BAG_ENCODE_JOB_FAILED ||
           state == BAG_ENCODE_JOB_CANCELLED;
}

void AssertPcmResultsEqual(const bag_pcm16_result& lhs,
                           const bag_pcm16_result& rhs,
                           const std::string& message) {
    test::AssertEq(lhs.sample_count, rhs.sample_count, message + " sample count should match.");
    const bool samples_match =
        lhs.sample_count == rhs.sample_count &&
        (lhs.sample_count == 0 ||
         std::equal(lhs.samples, lhs.samples + lhs.sample_count, rhs.samples));
    test::AssertTrue(samples_match, message + " samples should match exactly.");
}

EncodeJobCompletion WaitForEncodeJobTerminal(bag_encode_job* job,
                                            bool cancel_when_running = false) {
    test::AssertTrue(job != nullptr, "Job handle should not be null while waiting for completion.");

    EncodeJobCompletion completion{};
    float previous_progress = 0.0f;
    bool first_poll = true;
    bool cancel_requested = false;
    bag_encode_job_phase previous_phase = BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
    for (int attempt = 0; attempt < 20000; ++attempt) {
        bag_encode_job_progress progress{};
        test::AssertEq(
            bag_poll_encode_text_job(job, &progress),
            BAG_OK,
            "Polling an active encode job should succeed.");
        if (!first_poll) {
            test::AssertTrue(
                progress.progress_0_to_1 + 1e-6f >= previous_progress,
                "Encode job progress should stay monotonic.");
            if (progress.progress_0_to_1 > previous_progress + 1e-6f) {
                ++completion.progress_advance_count;
            }
            completion.saw_phase_regression =
                completion.saw_phase_regression ||
                static_cast<int>(progress.phase) < static_cast<int>(previous_phase);
        }
        first_poll = false;
        previous_progress = progress.progress_0_to_1;
        previous_phase = progress.phase;
        completion.saw_running = completion.saw_running || progress.state == BAG_ENCODE_JOB_RUNNING;
        completion.saw_postprocessing =
            completion.saw_postprocessing || progress.phase == BAG_ENCODE_JOB_PHASE_POSTPROCESSING;

        if (cancel_when_running && !cancel_requested && progress.state == BAG_ENCODE_JOB_RUNNING) {
            test::AssertEq(
                bag_cancel_encode_text_job(job),
                BAG_OK,
                "Cancelling a running encode job should succeed.");
            cancel_requested = true;
        }

        if (IsEncodeJobTerminal(progress.state)) {
            completion.final_progress = progress;
            return completion;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    test::Fail("Encode job did not reach a terminal state before timeout.");
    return completion;
}

std::string BuildLongJobText() {
    return std::string(256, 'J');
}

void AssertRoundTripAcrossCorpus(const std::vector<test::CorpusCase>& corpus,
                                 bag_transport_mode mode) {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        const auto decoder_config = MakeDecoderConfig(config_case, mode);

        for (const auto& corpus_case : corpus) {
            bag_pcm16_result pcm{};
            const auto encode_code =
                bag_encode_text(&encoder_config, corpus_case.text.c_str(), &pcm);
            test::AssertEq(encode_code, BAG_OK, "C API encode should succeed across corpus and configs.");

            const auto decoded = DecodeViaApi(decoder_config, pcm);
            test::AssertEq(decoded.code, BAG_OK, "Polling should succeed after valid push.");
            test::AssertEq(decoded.text, corpus_case.text, "API roundtrip should preserve original text.");
            test::AssertEq(decoded.mode, mode, "API result mode should match the configured mode.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

void TestApiFlashRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::FlashCorpusCases(), BAG_TRANSPORT_FLASH);
}

void TestApiProRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::ProCorpusCases(), BAG_TRANSPORT_PRO);
}

void TestApiUltraRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::UltraCorpusCases(), BAG_TRANSPORT_ULTRA);
}

void TestApiEncodeRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_encode_text(nullptr, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Null encoder config should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, nullptr, &pcm),
        BAG_INVALID_ARGUMENT,
        "Null text should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, "A", nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output PCM result should be rejected.");

    auto invalid_config = encoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero sample rate should be rejected.");

    invalid_config = encoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero frame size should be rejected.");

    invalid_config = encoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Unknown encoder mode should be rejected.");
}

void TestApiEncodeJobRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_encode_job* job = nullptr;
    bag_encode_job_progress progress{};
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_start_encode_text_job(nullptr, "A", &job),
        BAG_INVALID_ARGUMENT,
        "Null encoder config should be rejected for async jobs.");
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, nullptr, &job),
        BAG_INVALID_ARGUMENT,
        "Null text should be rejected for async jobs.");
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, "A", nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output job pointer should be rejected for async jobs.");

    auto invalid_config = encoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_start_encode_text_job(&invalid_config, "A", &job),
        BAG_INVALID_ARGUMENT,
        "Invalid configs should not create async jobs.");
    test::AssertTrue(job == nullptr, "Invalid async job start should not leave a job handle behind.");

    test::AssertEq(
        bag_poll_encode_text_job(nullptr, &progress),
        BAG_INVALID_ARGUMENT,
        "Polling with a null job should be rejected.");
    test::AssertEq(
        bag_poll_encode_text_job(job, nullptr),
        BAG_INVALID_ARGUMENT,
        "Polling with a null output should be rejected.");
    test::AssertEq(
        bag_cancel_encode_text_job(nullptr),
        BAG_INVALID_ARGUMENT,
        "Cancelling a null job should be rejected.");
    test::AssertEq(
        bag_take_encode_text_job_result(nullptr, &pcm),
        BAG_INVALID_ARGUMENT,
        "Taking a result from a null job should be rejected.");
    test::AssertEq(
        bag_take_encode_text_job_result(job, nullptr),
        BAG_INVALID_ARGUMENT,
        "Taking a result into a null PCM result should be rejected.");
}

void TestApiCreateDecoderRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case);
    bag_decoder* decoder = nullptr;

    test::AssertEq(
        bag_create_decoder(nullptr, &decoder),
        BAG_INVALID_ARGUMENT,
        "Null decoder config should be rejected.");
    test::AssertEq(
        bag_create_decoder(&decoder_config, nullptr),
        BAG_INVALID_ARGUMENT,
        "Null decoder output should be rejected.");

    auto invalid_config = decoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero decoder sample rate should be rejected.");

    invalid_config = decoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero decoder frame size should be rejected.");

    invalid_config = decoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Unknown decoder mode should be rejected.");
}

void TestApiPollAndResetLifecycle() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RESET", &pcm),
        BAG_OK,
        "Encoding for lifecycle test should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation for lifecycle test should succeed.");

    std::vector<char> text_buffer(256, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling before push should return not ready.");

    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "PCM push should succeed before reset.");
    bag_reset(decoder);
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling after reset should return not ready.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiModeSpecificValidation() {
    const auto config_case = test::ConfigCases().front();
    bag_pcm16_result pcm{};

    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_encode_text(&pro_config, pro_non_ascii.c_str(), &pcm),
        BAG_INVALID_ARGUMENT,
        "Pro mode should reject non-ASCII input.");
    test::AssertEq(
        bag_encode_text(&pro_config, test::BuildTooLongProCorpus().c_str(), &pcm),
        BAG_OK,
        "Pro mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_encode_text(&ultra_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
        BAG_OK,
        "Ultra mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);
}

void TestApiBoundarySuccessCases() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto long_flash_text = std::string(513, 'F');
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, long_flash_text.c_str(), &pcm),
            BAG_OK,
            "Flash mode should not inherit the single-frame payload limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Flash long-text decode should succeed.");
        test::AssertEq(decoded.text, long_flash_text, "Flash long-text roundtrip should preserve text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_FLASH, "Flash long-text decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongProCorpus().c_str(), &pcm),
            BAG_OK,
            "Pro mode should accept extended ASCII text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Pro boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongProCorpus(),
            "Pro boundary roundtrip should preserve the extended ASCII text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_PRO, "Pro boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
            BAG_OK,
            "Ultra mode should accept extended UTF-8 text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Ultra boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongUltraCorpus(),
            "Ultra boundary roundtrip should preserve the extended UTF-8 text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_ULTRA, "Ultra boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiEncodeJobSuccessAndProgressAcrossModes() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "job-progress";

    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };
    for (const bag_transport_mode mode : modes) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);

        bag_encode_job* job = nullptr;
        test::AssertEq(
            bag_start_encode_text_job(&encoder_config, text.c_str(), &job),
            BAG_OK,
            "Starting an async encode job should succeed for each transport mode.");
        const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
        test::AssertEq(
            completion.final_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Async encode jobs should reach the succeeded terminal state.");
        test::AssertEq(
            completion.final_progress.terminal_code,
            BAG_OK,
            "Successful async encode jobs should publish an OK terminal code.");
        test::AssertEq(
            completion.final_progress.progress_0_to_1,
            1.0f,
            "Successful async encode jobs should finish at 100% progress.");
        test::AssertTrue(
            !completion.saw_phase_regression,
            "Encode job phases should not move backwards while polling.");
        if (mode == BAG_TRANSPORT_FLASH) {
            test::AssertTrue(
                completion.saw_postprocessing,
                "Flash encode jobs should report a postprocessing phase.");
        } else {
            test::AssertTrue(
                !completion.saw_postprocessing,
                "Non-flash encode jobs should not report a postprocessing phase.");
        }

        bag_pcm16_result expected_pcm{};
        bag_pcm16_result actual_pcm{};
        bag_pcm16_result repeated_pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, text.c_str(), &expected_pcm),
            BAG_OK,
            "The synchronous encode baseline should succeed for async-job comparisons.");
        test::AssertEq(
            bag_take_encode_text_job_result(job, &actual_pcm),
            BAG_OK,
            "Succeeded async jobs should expose their PCM result.");
        test::AssertEq(
            bag_take_encode_text_job_result(job, &repeated_pcm),
            BAG_OK,
            "Succeeded async jobs should allow the result to be taken repeatedly.");
        AssertPcmResultsEqual(
            expected_pcm,
            actual_pcm,
            "Async job PCM should match the one-shot encode output.");
        AssertPcmResultsEqual(
            expected_pcm,
            repeated_pcm,
            "Repeated async job result retrieval should stay stable.");

        bag_encode_job_progress repeated_progress{};
        test::AssertEq(
            bag_poll_encode_text_job(job, &repeated_progress),
            BAG_OK,
            "Polling after completion should remain safe.");
        test::AssertEq(
            repeated_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Polling after success should stay in the succeeded state.");
        test::AssertEq(
            bag_cancel_encode_text_job(job),
            BAG_OK,
            "Cancelling a completed async encode job should stay idempotently OK.");

        bag_free_pcm16_result(&expected_pcm);
        bag_free_pcm16_result(&actual_pcm);
        bag_free_pcm16_result(&repeated_pcm);
        bag_destroy_encode_text_job(job);
    }
}

void TestApiEncodeJobImmediateCancel() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before immediate cancel.");
    test::AssertEq(
        bag_cancel_encode_text_job(job),
        BAG_OK,
        "Immediate cancel should succeed.");
    test::AssertEq(
        bag_cancel_encode_text_job(job),
        BAG_OK,
        "Repeated immediate cancel should stay idempotently OK.");

    const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
    test::AssertEq(
        completion.final_progress.state,
        BAG_ENCODE_JOB_CANCELLED,
        "Immediately cancelled jobs should report a cancelled terminal state.");
    test::AssertEq(
        completion.final_progress.terminal_code,
        BAG_CANCELLED,
        "Immediately cancelled jobs should expose the cancelled terminal code.");

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_take_encode_text_job_result(job, &pcm),
        BAG_CANCELLED,
        "Cancelled jobs should not expose a PCM result.");
    test::AssertTrue(pcm.samples == nullptr, "Cancelled jobs should not allocate a PCM buffer.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "Cancelled jobs should report no PCM samples.");

    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobCancelWhileRunning() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before running cancel.");

    const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job, true);
    test::AssertTrue(completion.saw_running, "The long async job should have entered the running state.");
    test::AssertEq(
        completion.final_progress.state,
        BAG_ENCODE_JOB_CANCELLED,
        "Cancelling while running should report the cancelled terminal state.");
    test::AssertEq(
        completion.final_progress.terminal_code,
        BAG_CANCELLED,
        "Cancelling while running should publish the cancelled terminal code.");

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_take_encode_text_job_result(job, &pcm),
        BAG_CANCELLED,
        "Running-cancelled jobs should not expose a PCM result.");
    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobDestroyWhileRunningIsSafe() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

    bag_encode_job* job = nullptr;
    test::AssertEq(
        bag_start_encode_text_job(&encoder_config, BuildLongJobText().c_str(), &job),
        BAG_OK,
        "Starting a long async flash job should succeed before destroy.");
    bag_destroy_encode_text_job(job);
}

void TestApiEncodeJobPublishesMultipleIntermediateProgressUpdates() {
    const auto config_case = test::ConfigCases().front();
    const std::string long_text(4096, 'P');
    const std::array<bag_transport_mode, 3> modes = {
        BAG_TRANSPORT_FLASH,
        BAG_TRANSPORT_PRO,
        BAG_TRANSPORT_ULTRA,
    };

    for (const bag_transport_mode mode : modes) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        bag_encode_job* job = nullptr;
        test::AssertEq(
            bag_start_encode_text_job(&encoder_config, long_text.c_str(), &job),
            BAG_OK,
            "Starting a long async encode job should succeed for progress sampling tests.");

        const EncodeJobCompletion completion = WaitForEncodeJobTerminal(job);
        test::AssertEq(
            completion.final_progress.state,
            BAG_ENCODE_JOB_SUCCEEDED,
            "Long async encode jobs should complete successfully.");
        test::AssertTrue(
            completion.progress_advance_count >= 3,
            "Long async encode jobs should publish multiple intermediate progress updates.");
        test::AssertTrue(
            !completion.saw_phase_regression,
            "Long async encode jobs should keep phase order monotonic.");
        bag_destroy_encode_text_job(job);
    }
}

void TestApiFlashConfigAffectsLengthAndRoundTrip() {
    const std::string text = "Length";

    for (const auto& config_case : test::ConfigCases()) {
        const auto coded_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto coded_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto ritual_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
        const auto ritual_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

        bag_pcm16_result coded_pcm{};
        bag_pcm16_result ritual_pcm{};
        test::AssertEq(
            bag_encode_text(&coded_encoder, text.c_str(), &coded_pcm),
            BAG_OK,
            "coded_burst flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
            BAG_OK,
            "ritual_chant flash encode should succeed through the C API.");
        test::AssertEq(
            coded_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST),
            "coded_burst C API flash length should stay on the baseline explicit configuration.");
        test::AssertEq(
            ritual_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT),
            "ritual_chant C API flash length should include the longer timing and shell configuration.");
        test::AssertTrue(
            ritual_pcm.sample_count > coded_pcm.sample_count,
            "ritual_chant flash output should be longer than coded_burst for the same text.");

        const auto coded_decoded = DecodeViaApi(coded_decoder, coded_pcm);
        const auto ritual_decoded = DecodeViaApi(ritual_decoder, ritual_pcm);
        test::AssertEq(coded_decoded.code, BAG_OK, "coded_burst flash decode should succeed.");
        test::AssertEq(ritual_decoded.code, BAG_OK, "ritual_chant flash decode should succeed.");
        test::AssertEq(coded_decoded.text, text, "coded_burst flash decode should preserve text.");
        test::AssertEq(ritual_decoded.text, text, "ritual_chant flash decode should preserve text.");

        bag_free_pcm16_result(&coded_pcm);
        bag_free_pcm16_result(&ritual_pcm);
    }
}

void TestApiFlashDecodeRequiresMatchingConfig() {
    const auto config_case = test::ConfigCases().front();
    const auto ritual_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
    const auto wrong_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
            BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
    const std::string text = "Mismatch";

    bag_pcm16_result ritual_pcm{};
    test::AssertEq(
        bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
        BAG_OK,
        "ritual_chant flash encode should succeed before wrong-style decode validation.");

    const auto decoded = DecodeViaApi(wrong_decoder, ritual_pcm);
    test::AssertTrue(
        decoded.code != BAG_OK || decoded.text != text,
        "Decoding ritual flash PCM with coded_burst signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&ritual_pcm);
}

void TestApiFreePcmResultIsIdempotent() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "free", &pcm),
        BAG_OK,
        "PCM result should be allocated for a successful encode.");
    test::AssertTrue(pcm.samples != nullptr, "PCM result buffer should be allocated.");
    test::AssertTrue(pcm.sample_count > 0, "PCM result sample count should be non-zero.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM result samples should clear after free.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "PCM result count should clear after free.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM free should remain safe on a cleared result.");
    test::AssertEq(
        pcm.sample_count,
        static_cast<size_t>(0),
        "PCM sample count should remain zero on repeated free.");
}

void TestApiVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "C API should expose the current release version.");
}

void TestApiValidationHelpers() {
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
    test::AssertTrue(
        bag_try_parse_transport_mode("flash", &mode) != 0,
        "Mode parser should accept flash.");
    test::AssertEq(mode, BAG_TRANSPORT_FLASH, "Mode parser should map flash correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("pro", &mode) != 0,
        "Mode parser should accept pro.");
    test::AssertEq(mode, BAG_TRANSPORT_PRO, "Mode parser should map pro correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("ultra", &mode) != 0,
        "Mode parser should accept ultra.");
    test::AssertEq(mode, BAG_TRANSPORT_ULTRA, "Mode parser should map ultra correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("warp", &mode) == 0,
        "Mode parser should reject unsupported values.");

    test::AssertEq(
        std::string(bag_transport_mode_name(BAG_TRANSPORT_PRO)),
        std::string("pro"),
        "Mode name helper should return the stable CLI spelling.");

    const auto config_case = test::ConfigCases().front();
    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, pro_non_ascii.c_str()),
        BAG_VALIDATION_PRO_ASCII_ONLY,
        "Validation helper should expose the pro ASCII-only rule.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_PRO_ASCII_ONLY),
        "ASCII",
        "Validation helper message should explain the ASCII-only rule.");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, test::BuildTooLongProCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that pro no longer inherits the compat frame limit.");

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_validate_encode_request(&ultra_config, test::BuildTooLongUltraCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that ultra no longer inherits the compat frame limit.");

    auto invalid_decoder = MakeDecoderConfig(config_case, static_cast<bag_transport_mode>(99));
    test::AssertEq(
        bag_validate_decoder_config(&invalid_decoder),
        BAG_VALIDATION_INVALID_MODE,
        "Decoder validation helper should reject unknown modes.");

    auto invalid_flash_encoder = MakeEncoderConfig(config_case);
    invalid_flash_encoder.flash_signal_profile = static_cast<bag_flash_signal_profile>(99);
    test::AssertEq(
        bag_validate_encode_request(&invalid_flash_encoder, "flash-style"),
        BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE,
        "Validation helper should reject unsupported flash signal profiles.");
    auto invalid_flash_decoder = MakeDecoderConfig(config_case);
    invalid_flash_decoder.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(99);
    test::AssertEq(
        bag_validate_decoder_config(&invalid_flash_decoder),
        BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR,
        "Decoder validation helper should reject unsupported flash voicing flavors.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE),
        "signal profile",
        "Validation helper message should explain the flash signal-profile failure.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR),
        "voicing flavor",
        "Validation helper message should explain the flash voicing-flavor failure.");
    test::AssertContains(
        bag_error_code_message(BAG_INTERNAL),
        "Internal",
        "Error message helper should expose a stable internal-error prompt.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Api.FlashRoundTripAcrossCorpusAndConfigs", TestApiFlashRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.ProRoundTripAcrossCorpusAndConfigs", TestApiProRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.UltraRoundTripAcrossCorpusAndConfigs", TestApiUltraRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.EncodeRejectsInvalidArguments", TestApiEncodeRejectsInvalidArguments);
    runner.Add("Api.EncodeJobRejectsInvalidArguments", TestApiEncodeJobRejectsInvalidArguments);
    runner.Add("Api.CreateDecoderRejectsInvalidArguments", TestApiCreateDecoderRejectsInvalidArguments);
    runner.Add("Api.PollAndResetLifecycle", TestApiPollAndResetLifecycle);
    runner.Add("Api.ModeSpecificValidation", TestApiModeSpecificValidation);
    runner.Add("Api.BoundarySuccessCases", TestApiBoundarySuccessCases);
    runner.Add("Api.EncodeJobSuccessAndProgressAcrossModes", TestApiEncodeJobSuccessAndProgressAcrossModes);
    runner.Add("Api.EncodeJobImmediateCancel", TestApiEncodeJobImmediateCancel);
    runner.Add("Api.EncodeJobCancelWhileRunning", TestApiEncodeJobCancelWhileRunning);
    runner.Add("Api.EncodeJobDestroyWhileRunningIsSafe", TestApiEncodeJobDestroyWhileRunningIsSafe);
    runner.Add(
        "Api.EncodeJobPublishesMultipleIntermediateProgressUpdates",
        TestApiEncodeJobPublishesMultipleIntermediateProgressUpdates);
    runner.Add("Api.FlashConfigAffectsLengthAndRoundTrip", TestApiFlashConfigAffectsLengthAndRoundTrip);
    runner.Add("Api.FlashDecodeRequiresMatchingConfig", TestApiFlashDecodeRequiresMatchingConfig);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    runner.Add("Api.ValidationHelpers", TestApiValidationHelpers);
    return runner.Run();
}
