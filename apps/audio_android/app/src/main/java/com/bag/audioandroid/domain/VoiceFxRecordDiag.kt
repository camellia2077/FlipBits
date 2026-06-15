package com.bag.audioandroid.domain

import com.bag.audioandroid.util.safeDebugLog

internal object VoiceFxRecordDiag {
    const val Tag = "VoiceFxRecordDiag"

    fun log(message: String) {
        safeDebugLog(Tag, message)
    }

    fun shouldLogBlock(blockIndex: Int): Boolean =
        blockIndex <= 6 ||
            blockIndex % 25 == 0
}
