#pragma once

#include <vector>

#include "bag/common/error_code.h"
#include "bag/common/types.h"

namespace bag::fun {

class IFunPhy {
public:
    virtual ~IFunPhy() = default;

    virtual ErrorCode Modulate(const std::vector<unsigned char>& payload,
                               std::vector<float>* out_pcm) = 0;
    virtual ErrorCode Demodulate(const PcmBlock& block, IrPacket* out_packet) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag::fun
