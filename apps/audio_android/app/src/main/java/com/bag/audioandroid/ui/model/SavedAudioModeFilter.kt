package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem

enum class SavedAudioModeFilter(
    @param:StringRes val labelResId: Int,
    val mode: TransportModeOption?
) {
    All(
        labelResId = R.string.common_all,
        mode = null
    ),
    Flash(
        labelResId = R.string.transport_mode_flash_label,
        mode = TransportModeOption.Flash
    ),
    Pro(
        labelResId = R.string.transport_mode_pro_label,
        mode = TransportModeOption.Pro
    ),
    Ultra(
        labelResId = R.string.transport_mode_ultra_label,
        mode = TransportModeOption.Ultra
    );

    fun matches(item: SavedAudioItem): Boolean =
        mode == null || item.modeWireName == mode.wireName

    companion object {
        fun fromTransportMode(mode: TransportModeOption): SavedAudioModeFilter =
            entries.firstOrNull { it.mode == mode } ?: All

        @StringRes
        fun labelResIdForModeWireName(modeWireName: String): Int =
            TransportModeOption.entries
                .firstOrNull { it.wireName == modeWireName }
                ?.labelResId
                ?: R.string.common_unknown
    }
}
