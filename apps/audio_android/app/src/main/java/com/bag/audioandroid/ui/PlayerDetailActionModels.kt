package com.bag.audioandroid.ui

import androidx.annotation.StringRes

internal data class PlayerDetailTopBarActions(
    @param:StringRes val modeLabelResId: Int,
    val onCollapse: () -> Unit,
    val onShareAudio: (() -> Unit)? = null,
    val onDownloadToDevice: (() -> Unit)? = null,
)

internal data class PlayerDetailBottomActions(
    val onOpenSavedAudioSheet: () -> Unit,
    val onSaveToLibrary: (() -> Unit)? = null,
    val isAlreadySavedToLibrary: Boolean = false,
)
