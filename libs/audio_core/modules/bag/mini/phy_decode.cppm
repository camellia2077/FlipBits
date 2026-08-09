module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.mini.phy_decode;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.mini.morse_rules;
export import bag.transport.decoder;

export namespace bag::mini_mode {

ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const MorseToneConfig& config,
                               std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::mini_mode
