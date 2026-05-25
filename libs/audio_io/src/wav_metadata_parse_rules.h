#pragma once

#include <cstddef>
#include <cstdint>

#include "wav_io.h"

namespace audio_io::detail::bytes_impl {

FlipBitsAudioMetadataParseResult ParseFlipBitsMetadataChunk(
    const std::uint8_t* chunk_data, std::size_t chunk_size);
FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadataBytes(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count);

}  // namespace audio_io::detail::bytes_impl
