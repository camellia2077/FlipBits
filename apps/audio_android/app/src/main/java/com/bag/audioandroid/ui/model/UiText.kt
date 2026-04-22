package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data object Empty : UiText

    data class Plain(
        val value: String,
    ) : UiText

    data class Resource(
        @param:StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText
}

@Composable
fun UiText.asString(): String =
    when (this) {
        UiText.Empty -> ""
        is UiText.Plain -> value
        is UiText.Resource ->
            if (formatArgs.isEmpty()) {
                stringResource(resId)
            } else {
                val resolvedArgs = ArrayList<Any>(formatArgs.size)
                for (arg in formatArgs) {
                    resolvedArgs +=
                        if (arg is UiText) {
                            arg.asString()
                        } else {
                            arg
                        }
                    }
                stringResource(resId, *resolvedArgs.toTypedArray())
            }
    }
