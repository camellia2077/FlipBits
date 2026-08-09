module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.phy_decode;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.ultra.phy_rules;
export import bag.transport.decoder;

export namespace bag::ultra {

ErrorCode DecodeSymbolsToPayload(
    const std::vector<std::uint8_t>& symbols,
    std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePcm16ToSymbols(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_symbols);
ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text, Mfsk16Speed speed);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::ultra
