#include <string>

#include "api_test_support.h"

namespace {

using namespace api_tests;

void TestApiFlashConfigAffectsLengthAndRoundTrip() {
    const std::string text = "Length";

    for (const auto& config_case : test::ConfigCases()) {
        const auto steady_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY);
        const auto steady_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY);
        const auto litany_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY);
        const auto litany_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY);
        const auto hostile_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE);
        const auto hostile_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE);

        bag_pcm16_result steady_pcm{};
        bag_pcm16_result litany_pcm{};
        bag_pcm16_result hostile_pcm{};
        test::AssertEq(
            bag_encode_text(&steady_encoder, text.c_str(), &steady_pcm),
            BAG_OK,
            "steady flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&litany_encoder, text.c_str(), &litany_pcm),
            BAG_OK,
            "litany flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&hostile_encoder, text.c_str(), &hostile_pcm),
            BAG_OK,
            "hostile flash encode should succeed through the C API.");
        test::AssertEq(
            steady_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                BAG_FLASH_VOICING_FLAVOR_STEADY),
            "steady C API flash length should stay on the baseline explicit configuration.");
        test::AssertEq(
            litany_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_LITANY,
                BAG_FLASH_VOICING_FLAVOR_LITANY),
            "litany C API flash length should include the longer timing and shell configuration.");
        test::AssertTrue(
            litany_pcm.sample_count > steady_pcm.sample_count,
            "litany flash output should be longer than steady for the same text.");
        test::AssertEq(
            hostile_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_HOSTILE,
                BAG_FLASH_VOICING_FLAVOR_HOSTILE),
            "hostile C API flash length should use baseline signal timing with its own shell.");

        const auto steady_decoded = DecodeViaApi(steady_decoder, steady_pcm);
        const auto litany_decoded = DecodeViaApi(litany_decoder, litany_pcm);
        const auto hostile_decoded = DecodeViaApi(hostile_decoder, hostile_pcm);
        test::AssertEq(steady_decoded.code, BAG_OK, "steady flash decode should succeed.");
        test::AssertEq(litany_decoded.code, BAG_OK, "litany flash decode should succeed.");
        test::AssertEq(hostile_decoded.code, BAG_OK, "hostile flash decode should succeed.");
        test::AssertEq(steady_decoded.text, text, "steady flash decode should preserve text.");
        test::AssertEq(litany_decoded.text, text, "litany flash decode should preserve text.");
        test::AssertEq(hostile_decoded.text, text, "hostile flash decode should preserve text.");

        bag_free_pcm16_result(&steady_pcm);
        bag_free_pcm16_result(&litany_pcm);
        bag_free_pcm16_result(&hostile_pcm);
    }
}

void TestApiFlashVoicingEmotionValuesRoundTrip() {
    struct EmotionCase {
        bag_flash_voicing_flavor flavor;
        const char* name;
    };
    const EmotionCase cases[] = {
        {BAG_FLASH_VOICING_FLAVOR_STEADY, "steady"},
        {BAG_FLASH_VOICING_FLAVOR_LITANY, "litany"},
        {BAG_FLASH_VOICING_FLAVOR_HOSTILE, "hostile"},
        {BAG_FLASH_VOICING_FLAVOR_COLLAPSE, "collapse"},
    };
    const auto config_case = test::ConfigCases().front();
    const std::string text = "Emotion";

    for (const auto& item : cases) {
        const auto encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                item.flavor);
        const auto decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_STEADY,
                item.flavor);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_validate_encode_request(&encoder, text.c_str()),
            BAG_VALIDATION_OK,
            std::string(item.name) + " flash voicing flavor should validate.");
        test::AssertEq(
            bag_encode_text(&encoder, text.c_str(), &pcm),
            BAG_OK,
            std::string(item.name) + " flash voicing flavor should encode.");
        const auto decoded = DecodeViaApi(decoder, pcm);
        test::AssertEq(
            decoded.code,
            BAG_OK,
            std::string(item.name) + " flash voicing flavor should decode.");
        test::AssertEq(
            decoded.text,
            text,
            std::string(item.name) + " flash voicing flavor should preserve text.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiFlashHostileKeepsSteadyLengthAndCollapseUsesVariableSilence() {
    const auto config_case = test::ConfigCases().front();
    const std::string text = "Shell collapse";
    const auto steady_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_STEADY);
    const auto hostile_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_HOSTILE);
    const auto collapse_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_COLLAPSE);
    const auto collapse_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_COLLAPSE);

    bag_pcm16_result steady_pcm{};
    bag_pcm16_result hostile_pcm{};
    bag_pcm16_result collapse_pcm{};
    test::AssertEq(bag_encode_text(&steady_encoder, text.c_str(), &steady_pcm), BAG_OK,
                   "steady flash encode should succeed.");
    test::AssertEq(bag_encode_text(&hostile_encoder, text.c_str(), &hostile_pcm), BAG_OK,
                   "hostile flash encode should succeed.");
    test::AssertEq(bag_encode_text(&collapse_encoder, text.c_str(), &collapse_pcm), BAG_OK,
                   "collapse flash encode should succeed.");
    test::AssertEq(
        hostile_pcm.sample_count,
        steady_pcm.sample_count,
        "hostile should reuse steady shell length in the minimal implementation.");
    test::AssertTrue(
        collapse_pcm.sample_count > steady_pcm.sample_count,
        "collapse should include variable hesitation silence in its payload length.");
    const auto collapse_decoded = DecodeViaApi(collapse_decoder, collapse_pcm);
    test::AssertEq(collapse_decoded.code, BAG_OK, "collapse flash decode should skip variable silence.");
    test::AssertEq(collapse_decoded.text, text, "collapse variable silence should preserve text.");
    bag_free_pcm16_result(&steady_pcm);
    bag_free_pcm16_result(&hostile_pcm);
    bag_free_pcm16_result(&collapse_pcm);
}

void TestApiFlashDecodeRequiresMatchingConfig() {
    const auto config_case = test::ConfigCases().front();
    const auto ritual_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_LITANY,
            BAG_FLASH_VOICING_FLAVOR_LITANY);
    const auto wrong_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_STEADY,
            BAG_FLASH_VOICING_FLAVOR_STEADY);
    const std::string text = "Mismatch";

    bag_pcm16_result ritual_pcm{};
    test::AssertEq(
        bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
        BAG_OK,
        "litany flash encode should succeed before wrong-style decode validation.");

    const auto decoded = DecodeViaApi(wrong_decoder, ritual_pcm);
    test::AssertTrue(
        decoded.code != BAG_OK || decoded.text != text,
        "Decoding litany flash PCM with steady signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&ritual_pcm);
}

}  // namespace

namespace api_tests {

void RegisterApiFlashTests(test::Runner& runner) {
    runner.Add("Api.FlashConfigAffectsLengthAndRoundTrip", TestApiFlashConfigAffectsLengthAndRoundTrip);
    runner.Add("Api.FlashVoicingEmotionValuesRoundTrip", TestApiFlashVoicingEmotionValuesRoundTrip);
    runner.Add("Api.FlashHostileKeepsSteadyLengthAndCollapseUsesVariableSilence",
               TestApiFlashHostileKeepsSteadyLengthAndCollapseUsesVariableSilence);
    runner.Add("Api.FlashDecodeRequiresMatchingConfig", TestApiFlashDecodeRequiresMatchingConfig);
}

}  // namespace api_tests
