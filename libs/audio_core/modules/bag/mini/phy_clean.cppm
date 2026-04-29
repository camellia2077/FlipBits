module;

export module bag.mini.phy_clean;

import std;

export import bag.common.types;
export import bag.transport.decoder;

export namespace bag::mini_mode {

struct MorseToneConfig {
  double tone_freq_hz = 700.0;
  int sample_rate_hz = 44100;
  int unit_samples = 2205;
  double amplitude = 0.75;
};

MorseToneConfig MakeMorseToneConfig(const CoreConfig& config);

ErrorCode EncodePayloadToPcm16(
    const std::vector<std::uint8_t>& payload, const MorseToneConfig& config,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);
ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const MorseToneConfig& config,
                               std::vector<std::uint8_t>* out_payload);

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::mini_mode
