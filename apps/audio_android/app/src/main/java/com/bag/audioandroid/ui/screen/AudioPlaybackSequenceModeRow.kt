package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import androidx.compose.ui.unit.dp

@Composable
internal fun AudioPlaybackSequenceModeRow(
    playbackSequenceMode: PlaybackSequenceMode,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackModeIconButton(
            selected = playbackSequenceMode == PlaybackSequenceMode.Shuffle,
            onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.Shuffle) },
            contentDescription = stringResource(R.string.audio_action_shuffle_playback)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null
            )
        }
        PlaybackModeIconButton(
            selected = playbackSequenceMode == PlaybackSequenceMode.RepeatOne,
            onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.RepeatOne) },
            contentDescription = stringResource(R.string.audio_action_repeat_one)
        ) {
            Icon(
                imageVector = Icons.Rounded.RepeatOne,
                contentDescription = null
            )
        }
        PlaybackModeIconButton(
            selected = playbackSequenceMode == PlaybackSequenceMode.RepeatList,
            onClick = { onPlaybackSequenceModeSelected(PlaybackSequenceMode.RepeatList) },
            contentDescription = stringResource(R.string.audio_action_repeat_list)
        ) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun PlaybackModeIconButton(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}
