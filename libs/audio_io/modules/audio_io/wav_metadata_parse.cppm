module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

#include "../../src/wav_metadata_parse_rules.h"

export module audio_io.wav_metadata_parse;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export namespace audio_io::detail::wav_metadata_parse_rules {

using ::audio_io::detail::wav_metadata_parse_rules::
    IsSupportedFlashVoicingStyle;
using ::audio_io::detail::wav_metadata_parse_rules::IsSupportedInputSourceKind;
using ::audio_io::detail::wav_metadata_parse_rules::IsSupportedMetadataVersion;
using ::audio_io::detail::wav_metadata_parse_rules::IsSupportedMiniSpeedStyle;
using ::audio_io::detail::wav_metadata_parse_rules::IsSupportedMode;
using ::audio_io::detail::wav_metadata_parse_rules::IsValidCreatedAtIsoUtc;
using ::audio_io::detail::wav_metadata_parse_rules::IsValidSegmentSampleCounts;
using ::audio_io::detail::wav_metadata_parse_rules::IsValidVersionText;
using ::audio_io::detail::wav_metadata_parse_rules::
    ParseFlipBitsAudioMetadataBytes;
using ::audio_io::detail::wav_metadata_parse_rules::ParseFlipBitsMetadataChunk;

}  // namespace audio_io::detail::wav_metadata_parse_rules
