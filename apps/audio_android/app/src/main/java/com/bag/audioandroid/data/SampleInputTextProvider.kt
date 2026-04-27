package com.bag.audioandroid.data

import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
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
        flavor: SampleFlavor,
    ): SampleInput

    fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String>

    fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput?
}
