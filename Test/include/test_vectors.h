#pragma once

#include <string>
#include <vector>

namespace test {

struct ConfigCase {
    std::string name;
    int sample_rate_hz;
    int frame_samples;
};

struct CorpusCase {
    std::string name;
    std::string text;
};

inline std::string BuildLongCorpus() {
    std::string text;
    const std::string pattern = "WaveBits-Long-Corpus-0123456789|";
    while (text.size() < 128) {
        text += pattern;
    }
    text.resize(128);
    return text;
}

inline const std::vector<ConfigCase>& ConfigCases() {
    static const std::vector<ConfigCase> cases = {
        {"44k1", 44100, 2205},
        {"48k", 48000, 2400},
    };
    return cases;
}

inline const std::vector<CorpusCase>& CorpusCases() {
    static const std::vector<CorpusCase> cases = {
        {"single_char", "A"},
        {"ascii", "Hello-123"},
        {"punctuation", "WaveBits: encode & decode!"},
        {"utf8", u8"你好，WaveBits"},
        {"long_ascii", BuildLongCorpus()},
    };
    return cases;
}

inline constexpr char kExpectedCoreVersion[] = "0.1.1";

}  // namespace test
