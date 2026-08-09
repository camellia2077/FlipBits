module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.phy_encode;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.ultra.phy_rules;
export import bag.ultra.tone_renderer;

export namespace bag::ultra {

ErrorCode EncodePayloadToSymbols(
    const std::vector<std::uint8_t>& payload,
    const Mfsk16Config& config,
    std::vector<std::uint8_t>* out_symbols);
ErrorCode EncodePayloadToPcm16(const std::vector<std::uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            Mfsk16Speed speed);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink,
                            Mfsk16Speed speed);

}  // namespace bag::ultra
