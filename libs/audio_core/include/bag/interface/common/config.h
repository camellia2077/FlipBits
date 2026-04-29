#pragma once

#include <cstdint>

namespace bag {

enum class TransportMode : std::uint8_t {
  kMini = 0,
  kFlash = 1,
  kPro = 2,
  kUltra = 3,
};

inline constexpr bool IsValidTransportMode(TransportMode mode) {
  switch (mode) {
    case TransportMode::kMini:
    case TransportMode::kFlash:
    case TransportMode::kPro:
    case TransportMode::kUltra:
      return true;
    default:
      return false;
  }
}

inline constexpr bool IsFramedTransportMode(TransportMode mode) {
  return mode == TransportMode::kMini || mode == TransportMode::kPro ||
         mode == TransportMode::kUltra;
}

struct CoreConfig {
  int sample_rate_hz = 48000;
  int frame_samples = 480;
  bool enable_diagnostics = false;
  TransportMode mode = TransportMode::kFlash;
  int reserved = 0;
};

}  // namespace bag
