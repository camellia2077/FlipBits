package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption

@Composable
internal fun FlashVoicingSelectorSection(
    enabled: Boolean,
    isFlashVoicingEnabled: Boolean,
    selectedFlashVoicingStyle: FlashVoicingStyleOption,
    onFlashVoicingStyleSelected: (FlashVoicingStyleOption) -> Unit,
) {
    val effectiveSelectedStyle =
        if (isFlashVoicingEnabled) {
            selectedFlashVoicingStyle
        } else {
            FlashVoicingStyleOption.Standard
        }
    var isVoicingStyleSheetOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceSettingPickerRow(
            title = stringResource(R.string.audio_flash_voicing_style_title),
            value = stringResource(effectiveSelectedStyle.labelResId),
            contentDescription = stringResource(R.string.audio_action_select_flash_voicing_style),
            testTag = "flash-voicing-style-selector",
            enabled = enabled,
            onClick = { isVoicingStyleSheetOpen = true },
        )
        Text(
            text = stringResource(R.string.audio_flash_style_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (isVoicingStyleSheetOpen) {
        FlashPresetPickerSheet(
            selectedStyle = effectiveSelectedStyle,
            onStyleSelected = { option ->
                onFlashVoicingStyleSelected(option)
                isVoicingStyleSheetOpen = false
            },
            onDismiss = { isVoicingStyleSheetOpen = false },
        )
    }
}
