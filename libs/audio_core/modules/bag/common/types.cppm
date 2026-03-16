module;

export module bag.common.types;

import std;

export import bag.common.config;

export namespace bag {

enum class VisualizationRegionKind : std::uint8_t {
    kUnknown = 0,
    kLeadingShell = 1,
    kPayload = 2,
    kTrailingShell = 3,
};

struct PcmBlock {
    const std::int16_t* samples = nullptr;
    std::size_t sample_count = 0;
    std::int64_t timestamp_ms = 0;
};

struct IrPacket {
    std::vector<std::uint8_t> bits;
    std::int64_t timestamp_ms = 0;
    float confidence = 0.0f;
};

struct TextResult {
    std::string text;
    bool complete = false;
    float confidence = 0.0f;
    TransportMode mode = TransportMode::kFlash;
};

struct VisualizationFrame {
    int sample_offset = 0;
    int sample_count = 0;
    float rms = 0.0f;
    float peak = 0.0f;
    float brightness = 0.0f;
    VisualizationRegionKind region_kind = VisualizationRegionKind::kUnknown;
};

struct VisualizationResult {
    std::vector<VisualizationFrame> frames;
    int total_samples = 0;
    int sample_rate_hz = 0;
    int frame_stride_samples = 0;
};

}  // namespace bag
