void ClearApiVoiceFxResult(bag_voice_fx_result* out_result) {
  if (out_result == nullptr) {
    return;
  }

  out_result->final_mix.samples = nullptr;
  out_result->final_mix.sample_count = 0;
  out_result->main_voice.samples = nullptr;
  out_result->main_voice.sample_count = 0;
  out_result->subvoice.samples = nullptr;
  out_result->subvoice.sample_count = 0;
  out_result->signal_overlay.samples = nullptr;
  out_result->signal_overlay.sample_count = 0;
}