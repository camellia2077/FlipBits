package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.miniPlayerTextColors
import com.bag.audioandroid.ui.model.MiniPlayerLeadingIcon
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@Composable
internal fun MiniPlayerBar(
    model: MiniPlayerUiModel,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    onOpenDetails: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
) {
    val textColors = miniPlayerTextColors(containerColor)
    // Keep the mini-player fully opaque. A translucent card lets the screen content show through
    // and makes the title/subtitle harder to read while the dock is floating above the page.
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        // We force tonalElevation to 0.dp to ensure the Dock System (Player + Bottom Bar)
        // stays color-consistent and does not get tinted by Material 3's primary color.
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.boundsInRoot().topLeft
                    debugMiniPlayerBarLog(
                        "surface",
                        "leadingIcon=${model.leadingIcon.name} playing=$isPlaying " +
                            "x=${position.x.toInt()} y=${position.y.toInt()} " +
                            "w=${coordinates.size.width} h=${coordinates.size.height}",
                    )
                },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenDetails),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniPlayerLeadingIcon(model = model)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = model.title.asString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColors.titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = model.subtitle.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColors.subtitleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = onOpenSavedAudioSheet,
                colors = utilityActionIconButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.audio_action_open_saved_audio_list),
                )
            }
            FilledIconButton(
                onClick = onTogglePlayback,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription =
                        stringResource(
                            if (isPlaying) R.string.audio_action_pause else R.string.audio_action_play,
                        ),
                )
            }
        }
    }
}

private fun debugMiniPlayerBarLog(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d("MiniPlayerBarDiag", "$label $message")
    } catch (_: RuntimeException) {
    }
}

@Composable
private fun MiniPlayerLeadingIcon(model: MiniPlayerUiModel) {
    val visualTokens = appThemeVisualTokens()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = visualTokens.miniPlayerLeadingContainerColor,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector =
                    when (model.leadingIcon) {
                        MiniPlayerLeadingIcon.Generated -> Icons.Rounded.GraphicEq
                        MiniPlayerLeadingIcon.Saved -> Icons.Rounded.LibraryMusic
                    },
                contentDescription = null,
                tint = visualTokens.miniPlayerLeadingContentColor,
            )
        }
    }
}
