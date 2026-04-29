package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.theme.appThemeAccentTokens

@Composable
internal fun PlayerDetailDecodedSection(
    decodedPayload: DecodedPayloadViewData,
    isCodecBusy: Boolean,
    onDecodeAudio: () -> Unit,
    onClearDecodedText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var contentExpanded by rememberSaveable { mutableStateOf(true) }
    val accentTokens = appThemeAccentTokens()

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.audio_player_detail_decoded_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(
                        onClick = onClearDecodedText,
                        enabled =
                            decodedPayload.hasTextResult ||
                                decodedPayload.rawPayloadAvailable ||
                                decodedPayload.textDecodeStatusCode != BagDecodeContentCodes.STATUS_UNAVAILABLE,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.audio_action_clear_result),
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription =
                                stringResource(
                                    if (expanded) {
                                        R.string.audio_action_collapse_result
                                    } else {
                                        R.string.audio_action_expand_result
                                    },
                                ),
                            tint = accentTokens.disclosureAccentTint,
                        )
                    }
                    TextButton(
                        onClick = onDecodeAudio,
                        enabled = !isCodecBusy,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (isCodecBusy) {
                                        R.string.audio_action_decode_busy
                                    } else {
                                        R.string.audio_action_decode
                                    },
                                ),
                        )
                    }
                }
            }
            if (expanded) {
                DecodedPayloadContent(
                    accentTokens = accentTokens,
                    decodedPayload = decodedPayload,
                    emptyTextResId = R.string.audio_player_detail_decoded_empty,
                    bodyExpanded = contentExpanded,
                    onToggleBodyExpanded = { contentExpanded = !contentExpanded },
                )
            }
        }
    }
}
