package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class PaletteFamily(
    @param:StringRes val titleResId: Int,
) {
    Custom(R.string.palette_family_custom),
    Brand(R.string.palette_family_brand),
    Reds(R.string.palette_family_reds_pinks),
    Oranges(R.string.palette_family_oranges),
    Yellows(R.string.palette_family_yellows),
    Greens(R.string.palette_family_greens),
    Blues(R.string.palette_family_cyans_blues),
    Purples(R.string.palette_family_purples_magentas),
    Neutrals(R.string.palette_family_neutrals),
}
