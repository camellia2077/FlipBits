package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data object Empty : UiText

    data class Resource(
        @param:StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText
}

@Composable
fun UiText.asString(): String =
    when (this) {
        UiText.Empty -> ""
        is UiText.Resource ->
            if (formatArgs.isEmpty()) {
                stringResource(resId)
            } else {
                stringResource(resId, *formatArgs.toTypedArray())
            }
    }
