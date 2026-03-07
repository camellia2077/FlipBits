#pragma once

#include <vector>

#include "bag/common/error_code.h"
#include "bag/common/types.h"

namespace bag {

class ILinkLayer {
public:
    virtual ~ILinkLayer() = default;

    virtual ErrorCode PushIr(const IrPacket& packet) = 0;
    virtual ErrorCode PollPayload(std::vector<unsigned char>* out_payload) = 0;
    virtual void Reset() = 0;
};

}  // namespace bag
