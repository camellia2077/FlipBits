module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

export module bag.flash.codec;


export import bag.common.error_code;

export namespace bag::flash {

ErrorCode EncodeTextToBytes(const std::string& text,
                            std::vector<std::uint8_t>* out_bytes);
ErrorCode DecodeBytesToText(const std::vector<std::uint8_t>& bytes,
                            std::string* out_text);

}  // namespace bag::flash
