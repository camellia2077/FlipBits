package com.bag.audioandroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    // Most action buttons stay on the default container treatment. The optional border lets a
    // few stronger primary actions opt into a clearer selected / emphasized outline when needed.
    Surface(
        shape = MaterialTheme.shapes.medium,
        color =
            if (enabled) {
                visualTokens.actionContainerColor
            } else {
                visualTokens.actionDisabledContainerColor
            },
        border =
            if (borderColor != Color.Unspecified && borderWidth > 0.dp) {
                BorderStroke(borderWidth, borderColor)
            } else {
                null
            },
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color =
                    if (textColor != Color.Unspecified) {
                        textColor
                    } else if (enabled) {
                        visualTokens.actionContentColor
                    } else {
                        visualTokens.actionDisabledContentColor
                    },
            )
        }
    }
}
