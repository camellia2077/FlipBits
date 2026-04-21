package com.bag.audioandroid.data

import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption

data class SampleInput(
    val id: String,
    val text: String,
)

interface SampleInputTextProvider {
    fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
    ): SampleInput

    fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        length: SampleInputLengthOption,
        excludingSampleId: String? = null,
    ): SampleInput

    fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        sampleId: String,
    ): SampleInput?
}
