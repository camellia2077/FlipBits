package com.bag.audioandroid.ui

import com.bag.audioandroid.data.AndroidSampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState

class SampleInputSessionUpdater(
    private val sampleInputTextProvider: AndroidSampleInputTextProvider
) {
    fun initialize(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        language: AppLanguageOption
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            session.copy(inputText = sampleInputTextProvider.sampleText(mode, language))
        }

    fun refreshForLanguageChange(
        sessions: Map<TransportModeOption, ModeAudioSessionState>,
        previousLanguage: AppLanguageOption,
        newLanguage: AppLanguageOption
    ): Map<TransportModeOption, ModeAudioSessionState> =
        sessions.mapValues { (mode, session) ->
            val previousSample = sampleInputTextProvider.sampleText(mode, previousLanguage)
            val newSample = sampleInputTextProvider.sampleText(mode, newLanguage)
            if (session.inputText.isBlank() || session.inputText == previousSample) {
                session.copy(inputText = newSample)
            } else {
                session
            }
        }
}
