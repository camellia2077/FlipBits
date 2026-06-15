void bag_free_voice_fx_result(bag_voice_fx_result* result) {
  if (result == nullptr) {
    return;
  }

  bag_free_pcm16_result(&result->final_mix);
  bag_free_pcm16_result(&result->main_voice);
  bag_free_pcm16_result(&result->subvoice);
  bag_free_pcm16_result(&result->signal_overlay);
  ClearApiVoiceFxResult(result);
}