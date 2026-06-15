namespace {

bool IsValidVoiceFxPreset(const bag_voice_fx_preset preset) {
  return preset == BAG_VOICE_FX_MACHINE_VOICE ||
         preset == BAG_VOICE_FX_BINHARIC ||
         preset == BAG_VOICE_FX_SIGNAL_CANT ||
         preset == BAG_VOICE_FX_ROBOT_VOX ||
         preset == BAG_VOICE_FX_RAW_CONSTANT ||
         preset == BAG_VOICE_FX_VOICE_TRIGGER;
}

bool IsValidVoiceFxSubvoiceStyle(
    const bag_voice_fx_subvoice_style subvoice_style) {
  return subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD ||
         subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_LITANY ||
         subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY ||
         subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE ||
         subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL ||
         subvoice_style == BAG_VOICE_FX_SUBVOICE_STYLE_VOID;
}

bag::VoiceFxPreset ToCoreVoiceFxPreset(const bag_voice_fx_preset preset) {
  switch (preset) {
    case BAG_VOICE_FX_BINHARIC:
      return bag::VoiceFxPreset::kBinharic;
    case BAG_VOICE_FX_SIGNAL_CANT:
      return bag::VoiceFxPreset::kSignalCant;
    case BAG_VOICE_FX_ROBOT_VOX:
      return bag::VoiceFxPreset::kRobotVox;
    case BAG_VOICE_FX_RAW_CONSTANT:
      return bag::VoiceFxPreset::kRawConstant;
    case BAG_VOICE_FX_VOICE_TRIGGER:
      return bag::VoiceFxPreset::kVoiceTrigger;
    case BAG_VOICE_FX_MACHINE_VOICE:
    default:
      return bag::VoiceFxPreset::kMachineVoice;
  }
}

bag::VoiceFxSubvoiceStyle ToCoreVoiceFxSubvoiceStyle(
    const bag_voice_fx_subvoice_style subvoice_style) {
  switch (subvoice_style) {
    case BAG_VOICE_FX_SUBVOICE_STYLE_LITANY:
      return bag::VoiceFxSubvoiceStyle::kLitany;
    case BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY:
      return bag::VoiceFxSubvoiceStyle::kHostility;
    case BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE:
      return bag::VoiceFxSubvoiceStyle::kCollapse;
    case BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL:
      return bag::VoiceFxSubvoiceStyle::kZeal;
    case BAG_VOICE_FX_SUBVOICE_STYLE_VOID:
      return bag::VoiceFxSubvoiceStyle::kVoid;
    case BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD:
    default:
      return bag::VoiceFxSubvoiceStyle::kStandard;
  }
}

bool IsValidVoiceFxConfig(const bag_voice_fx_config* config) {
  return config != nullptr && config->sample_rate_hz > 0 &&
         IsValidVoiceFxPreset(config->preset) &&
         IsValidVoiceFxSubvoiceStyle(config->subvoice_style);
}

bag::VoiceFxConfig ToCoreVoiceFxConfig(const bag_voice_fx_config& config) {
  bag::VoiceFxConfig voice_config{};
  voice_config.sample_rate_hz = config.sample_rate_hz;
  voice_config.enable_diagnostics = config.enable_diagnostics != 0;
  voice_config.preset = ToCoreVoiceFxPreset(config.preset);
  voice_config.subvoice_style =
      ToCoreVoiceFxSubvoiceStyle(config.subvoice_style);
  return voice_config;
}

bag_error_code CopyVoiceFxResultToApi(const bag::VoiceFxResult& result,
                                      bag_voice_fx_result* out_result) {
  if (out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  ClearApiVoiceFxResult(out_result);
  bag_error_code code = CopyPcmToApiResult(result.final_mix, &out_result->final_mix);
  if (code != BAG_OK) {
    bag_free_voice_fx_result(out_result);
    return code;
  }

  code = CopyPcmToApiResult(result.main_voice, &out_result->main_voice);
  if (code != BAG_OK) {
    bag_free_voice_fx_result(out_result);
    return code;
  }

  code = CopyPcmToApiResult(result.subvoice, &out_result->subvoice);
  if (code != BAG_OK) {
    bag_free_voice_fx_result(out_result);
    return code;
  }

  code = CopyPcmToApiResult(result.signal_overlay, &out_result->signal_overlay);
  if (code != BAG_OK) {
    bag_free_voice_fx_result(out_result);
    return code;
  }
  return BAG_OK;
}

}  // namespace

bag_error_code bag_apply_voice_fx(const bag_voice_fx_config* config,
                                  const int16_t* samples,
                                  size_t sample_count,
                                  bag_voice_fx_result* out_result) {
  if (!IsValidVoiceFxConfig(config) || samples == nullptr || sample_count == 0 ||
      out_result == nullptr) {
    if (out_result != nullptr) {
      ClearApiVoiceFxResult(out_result);
    }
    return BAG_INVALID_ARGUMENT;
  }

  try {
    std::vector<std::int16_t> input(samples, samples + sample_count);
    const bag::VoiceFxConfig voice_config = ToCoreVoiceFxConfig(*config);
    bag::VoiceFxResult result;
    const bag::ErrorCode code = bag::ApplyVoiceFx(voice_config, input, &result);
    if (code != bag::ErrorCode::kOk) {
      ClearApiVoiceFxResult(out_result);
      return ToApiCode(code);
    }
    return CopyVoiceFxResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INTERNAL;
  }
}

bag_error_code bag_create_voice_fx_processor(
    const bag_voice_fx_config* config,
    bag_voice_fx_processor** out_processor) {
  if (!IsValidVoiceFxConfig(config) || out_processor == nullptr) {
    if (out_processor != nullptr) {
      *out_processor = nullptr;
    }
    return BAG_INVALID_ARGUMENT;
  }

  try {
    auto processor = std::make_unique<bag_voice_fx_processor>();
    processor->processor =
        std::make_unique<bag::VoiceFxProcessor>(ToCoreVoiceFxConfig(*config));
    *out_processor = processor.release();
    return BAG_OK;
  } catch (const std::invalid_argument&) {
    *out_processor = nullptr;
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    *out_processor = nullptr;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_process_voice_fx_block(
    bag_voice_fx_processor* processor,
    const int16_t* samples,
    size_t sample_count,
    bag_voice_fx_result* out_result) {
  if (processor == nullptr || processor->processor == nullptr ||
      samples == nullptr || sample_count == 0 || out_result == nullptr) {
    if (out_result != nullptr) {
      ClearApiVoiceFxResult(out_result);
    }
    return BAG_INVALID_ARGUMENT;
  }

  try {
    std::vector<std::int16_t> input(samples, samples + sample_count);
    bag::VoiceFxResult result;
    const bag::ErrorCode code =
        processor->processor->ProcessBlock(input, &result);
    if (code != bag::ErrorCode::kOk) {
      ClearApiVoiceFxResult(out_result);
      return ToApiCode(code);
    }
    return CopyVoiceFxResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INTERNAL;
  }
}

bag_error_code bag_flush_voice_fx_processor(
    bag_voice_fx_processor* processor,
    bag_voice_fx_result* out_result) {
  if (processor == nullptr || processor->processor == nullptr ||
      out_result == nullptr) {
    if (out_result != nullptr) {
      ClearApiVoiceFxResult(out_result);
    }
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::VoiceFxResult result;
    const bag::ErrorCode code = processor->processor->Flush(&result);
    if (code != bag::ErrorCode::kOk) {
      ClearApiVoiceFxResult(out_result);
      return ToApiCode(code);
    }
    return CopyVoiceFxResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiVoiceFxResult(out_result);
    return BAG_INTERNAL;
  }
}

void bag_destroy_voice_fx_processor(bag_voice_fx_processor* processor) {
  delete processor;
}
