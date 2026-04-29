package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun PlaybackDataFollowSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    transportMode: TransportModeOption?,
    modifier: Modifier = Modifier,
    initialAnnotationMode: PlaybackFollowViewMode = PlaybackFollowViewMode.Hex,
) {
    var selectedAnnotationMode by rememberSaveable { mutableStateOf(initialAnnotationMode.name) }
    val isMorseMode = transportMode == TransportModeOption.Mini
    val normalizedAnnotationModeName =
        if (isMorseMode) {
            PlaybackFollowViewMode.Morse.name
        } else {
            selectedAnnotationMode.takeUnless { it == PlaybackFollowViewMode.Morse.name }
                ?: PlaybackFollowViewMode.Hex.name
        }
    val presentationState =
        rememberPlaybackFollowPresentationState(
            followData = followData,
            displayedSamples = displayedSamples,
            selectedAnnotationModeName = normalizedAnnotationModeName,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-follow-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!followData.followAvailable) {
            Text(
                text = stringResource(R.string.audio_follow_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (!followData.textFollowAvailable || followData.textTokens.isEmpty()) {
            Text(
                text = stringResource(R.string.audio_follow_text_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        PlaybackFollowAnnotationModeSwitcher(
            selectedMode = presentationState.followViewMode,
            onModeSelected = { selectedAnnotationMode = it.name },
            transportMode = transportMode,
        )
        PlaybackFollowTokenStrip(
            followData = followData,
            presentationState = presentationState,
        )
    }
}
