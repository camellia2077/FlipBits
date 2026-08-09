#pragma once

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/common/types.h"
#include "android_bag/transport/decoder.h"
#include "android_bag/ultra/codec.h"

namespace bag::ultra {

struct Mfsk16Config {
    std::array<double, 16> freqs_hz = {
        1000.000, 1015.625, 1031.250, 1046.875,
        1062.500, 1078.125, 1093.750, 1109.375,
        1125.000, 1140.625, 1156.250, 1171.875,
        1187.500, 1203.125, 1218.750, 1234.375};
    int sample_rate_hz = 44100;
    int symbol_samples = 2822;
    double symbol_rate_baud = kMfsK16SymbolRateBaud;
    double amplitude = 0.8;
};

struct SymbolRenderProgress {
    std::size_t completed_work = 0;
    std::size_t total_work = 0;
    bool finished = false;
};

class SymbolRenderer {
 public:
    SymbolRenderer(std::uint8_t symbol,
                   std::size_t write_offset,
                   const Mfsk16Config& config,
                   std::vector<std::int16_t>* out_pcm,
                   double initial_phase = 0.0);
    SymbolRenderer(std::uint8_t symbol,
                   std::size_t write_offset,
                   const Mfsk16Config& config,
                   std::vector<std::int16_t>* out_pcm,
                   std::size_t symbol_sample_count,
                   double initial_phase);
    ~SymbolRenderer();

    std::size_t TotalWork() const;
    bool Finished() const;
    double FinalPhase() const;
    SymbolRenderProgress Pump(std::size_t work_budget);

 private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

Mfsk16Config MakeMfsk16Config(const CoreConfig& config);
Mfsk16Config MakeMfsk16Config(const CoreConfig& config, Mfsk16Speed speed);
bool IsValidMfsk16Config(const Mfsk16Config& config);
int NominalSymbolSamples(int sample_rate_hz,
                         double symbol_rate_baud = 15.625);
std::size_t SymbolBoundarySample(int sample_rate_hz,
                                 std::size_t symbol_index,
                                 double symbol_rate_baud = 15.625);
std::size_t TotalSamplesForSymbols(int sample_rate_hz,
                                   std::size_t symbol_count,
                                   double symbol_rate_baud = 15.625);
ErrorCode EncodePayloadToSymbols(
    const std::vector<std::uint8_t>& payload,
    const Mfsk16Config& config,
    std::vector<std::uint8_t>* out_symbols);
ErrorCode DecodeSymbolsToPayload(
    const std::vector<std::uint8_t>& symbols,
    std::vector<std::uint8_t>* out_payload);

ErrorCode EncodeSymbolsToPcm16(const std::vector<std::uint8_t>& symbols,
                               const Mfsk16Config& config,
                               std::vector<std::int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink = nullptr,
                               float progress_begin = 0.0f,
                               float progress_end = 1.0f);
ErrorCode DecodePcm16ToSymbols(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_symbols);

ErrorCode EncodePayloadToPcm16(const std::vector<std::uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<std::int16_t>* out_pcm);
ErrorCode DecodePcm16ToPayload(const std::vector<std::int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<std::uint8_t>* out_payload);

ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            Mfsk16Speed speed);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink,
                            Mfsk16Speed speed);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text,
                            Mfsk16Speed speed);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::ultra
