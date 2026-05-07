package com.bag.audioandroid.ui.state

data class FollowDataWindowSource(
    val segmentTexts: List<String>,
    val segmentSampleCounts: List<Int>,
    val totalPcmSampleCount: Int,
    val flashSignalProfile: Int = 0,
    val flashVoicingFlavor: Int = 0,
) {
    init {
        require(segmentTexts.size == segmentSampleCounts.size) {
            "Follow window source must keep one sample count per text segment."
        }
        require(segmentSampleCounts.all { it > 0 }) {
            "Follow window segment sample counts must be positive."
        }
    }
}

data class FollowDataWindowState(
    val startSample: Int = 0,
    val endSampleExclusive: Int = 0,
) {
    fun covers(sample: Int): Boolean = sample >= startSample && sample < endSampleExclusive
}
