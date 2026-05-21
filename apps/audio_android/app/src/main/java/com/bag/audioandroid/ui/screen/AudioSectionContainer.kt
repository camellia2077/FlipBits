package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun AudioSectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visualTokens = appThemeVisualTokens()
    val containerColor =
        visualTokens.groupContainerColor.takeIf { it.isSpecified }
            ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    val outlineColor =
        visualTokens.subtleOutlineColor.takeIf { it.isSpecified }
            ?: MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.42f)),
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}
