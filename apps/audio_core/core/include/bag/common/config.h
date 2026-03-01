#pragma once

#include <cstdint>

namespace bag {

struct CoreConfig {
    int sample_rate_hz = 48000;
    int frame_samples = 480;
    bool enable_diagnostics = false;
    int reserved = 0;
};

}  // namespace bag
