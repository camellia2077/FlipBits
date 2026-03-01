#include <jni.h>

#include <cstdint>
#include <string>
#include <vector>

#include "bag/fsk/fsk_codec.h"
#include "bag_api.h"

namespace {
constexpr int kDefaultSampleRateHz = 44100;
constexpr int kDefaultFrameSamples = 2205;

std::string JStringToStdString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    const std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

bag_decoder_config MakeConfig(int sample_rate_hz, int frame_samples) {
    bag_decoder_config config{};
    config.sample_rate_hz = sample_rate_hz > 0 ? sample_rate_hz : kDefaultSampleRateHz;
    config.frame_samples = frame_samples > 0 ? frame_samples : kDefaultFrameSamples;
    config.enable_diagnostics = 0;
    config.reserved = 0;
    return config;
}
}  // namespace

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeEncodeTextToPcm(
    JNIEnv* env, jobject /*thiz*/, jstring text, jint sample_rate_hz, jint frame_samples) {
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return env->NewShortArray(0);
    }

    bag::fsk::FskConfig fsk_config{};
    const bag_decoder_config config = MakeConfig(sample_rate_hz, frame_samples);
    fsk_config.sample_rate_hz = config.sample_rate_hz;
    fsk_config.bit_duration_sec =
        static_cast<double>(config.frame_samples) / static_cast<double>(config.sample_rate_hz);
    const std::vector<int16_t> pcm = bag::fsk::EncodeTextToPcm16(input, fsk_config);

    jshortArray out = env->NewShortArray(static_cast<jsize>(pcm.size()));
    if (out == nullptr || pcm.empty()) {
        return out;
    }
    env->SetShortArrayRegion(
        out, 0, static_cast<jsize>(pcm.size()), reinterpret_cast<const jshort*>(pcm.data()));
    return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodeGeneratedPcm(
    JNIEnv* env, jobject /*thiz*/, jshortArray pcm, jint sample_rate_hz, jint frame_samples) {
    if (pcm == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize len = env->GetArrayLength(pcm);
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));

    const bag_decoder_config config = MakeConfig(sample_rate_hz, frame_samples);
    bag_decoder* decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return env->NewStringUTF("");
    }

    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);

    char text_buffer[4096] = {0};
    bag_text_result result{};
    result.buffer = text_buffer;
    result.buffer_size = sizeof(text_buffer);
    (void)bag_poll_result(decoder, &result);

    bag_destroy_decoder(decoder);
    return env->NewStringUTF(text_buffer);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetCoreVersion(
    JNIEnv* env, jobject /*thiz*/) {
    const char* version = bag_core_version();
    if (version == nullptr) {
        return env->NewStringUTF("unknown");
    }
    return env->NewStringUTF(version);
}
