namespace bag::flash {

namespace {

float ClampUnit(double value) {
    return static_cast<float>(std::clamp(value, 0.0, 1.0));
}

VisualizationRegionKind RegionKindForFrame(int frame_begin,
                                           int frame_end,
                                           const FlashVoicingDescriptor& descriptor) {
    const int payload_begin = static_cast<int>(descriptor.leading_nonpayload_samples);
    const int payload_end = payload_begin + static_cast<int>(descriptor.payload_sample_count);
    if (frame_end <= payload_begin) {
        return VisualizationRegionKind::kLeadingShell;
    }
    if (frame_begin >= payload_end) {
        return VisualizationRegionKind::kTrailingShell;
    }
    if (frame_begin >= payload_begin && frame_end <= payload_end) {
        return VisualizationRegionKind::kPayload;
    }
    return VisualizationRegionKind::kUnknown;
}

VisualizationFrame AnalyzeFrame(const std::vector<std::int16_t>& pcm,
                                int frame_begin,
                                int frame_end,
                                const FlashVoicingDescriptor& descriptor) {
    VisualizationFrame frame{};
    frame.sample_offset = frame_begin;
    frame.sample_count = frame_end - frame_begin;
    frame.region_kind = RegionKindForFrame(frame_begin, frame_end, descriptor);
    if (frame.sample_count <= 0) {
        return frame;
    }

    double square_sum = 0.0;
    double peak = 0.0;
    double diff_sum = 0.0;

    for (int index = frame_begin; index < frame_end; ++index) {
        const double normalized =
            static_cast<double>(pcm[static_cast<std::size_t>(index)]) / 32767.0;
        const double absolute = std::abs(normalized);
        square_sum += normalized * normalized;
        peak = std::max(peak, absolute);
        if (index > frame_begin) {
            const double diff =
                static_cast<double>(pcm[static_cast<std::size_t>(index)]) -
                static_cast<double>(pcm[static_cast<std::size_t>(index - 1)]);
            diff_sum += std::abs(diff);
        }
    }

    frame.rms = ClampUnit(std::sqrt(square_sum / static_cast<double>(frame.sample_count)));
    frame.peak = ClampUnit(peak);
    frame.brightness = frame.sample_count > 1
                           ? ClampUnit(
                                 (diff_sum / static_cast<double>(frame.sample_count - 1)) /
                                 65535.0)
                           : 0.0f;
    return frame;
}

}  // namespace

ErrorCode AnalyzeVisualization(const CoreConfig& config,
                               const std::vector<std::int16_t>& pcm,
                               VisualizationResult* out_result) {
    if (out_result == nullptr || pcm.empty()) {
        return ErrorCode::kInvalidArgument;
    }
    out_result->frames.clear();
    out_result->total_samples = 0;
    out_result->sample_rate_hz = 0;
    out_result->frame_stride_samples = 0;

    if (config.sample_rate_hz <= 0 || config.frame_samples <= 0) {
        return ErrorCode::kInvalidArgument;
    }
    if (pcm.size() > static_cast<std::size_t>(std::numeric_limits<int>::max())) {
        return ErrorCode::kInvalidArgument;
    }

    try {
        const FlashVoicingConfig voicing_config =
            MakeFormalVoicingConfigForFlavor(config, config.flash_voicing_flavor);
        const FlashVoicingDescriptor descriptor =
            DescribeVoicingOutput(pcm.size(), voicing_config);
        const int frame_stride_samples = std::max(1, config.frame_samples / 4);
        const int total_samples = static_cast<int>(pcm.size());

        out_result->frames.reserve(
            static_cast<std::size_t>((total_samples + frame_stride_samples - 1) / frame_stride_samples));
        out_result->total_samples = total_samples;
        out_result->sample_rate_hz = config.sample_rate_hz;
        out_result->frame_stride_samples = frame_stride_samples;

        for (int frame_begin = 0; frame_begin < total_samples; frame_begin += frame_stride_samples) {
            const int frame_end = std::min(frame_begin + frame_stride_samples, total_samples);
            out_result->frames.push_back(AnalyzeFrame(pcm, frame_begin, frame_end, descriptor));
        }
        return ErrorCode::kOk;
    } catch (const std::invalid_argument&) {
        return ErrorCode::kInvalidArgument;
    } catch (...) {
        return ErrorCode::kInternal;
    }
}

}  // namespace bag::flash
