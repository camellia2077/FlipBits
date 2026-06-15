package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.VoiceFxPresetOption

@Composable
internal fun VoicePresetSelectorSection(
    enabled: Boolean,
    availablePresets: List<VoiceFxPresetOption>,
    selectedPreset: VoiceFxPresetOption,
    onPresetSelected: (VoiceFxPresetOption) -> Unit,
) {
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    val selectedPresetLabel = stringResource(selectedPreset.labelResId)
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        VoiceSettingPickerRow(
            title = stringResource(R.string.voice_preset_title),
            value = selectedPresetLabel,
            contentDescription = selectedPresetLabel,
            testTag = "voice-preset-selector",
            enabled = enabled,
            onClick = { isSheetOpen = true },
        )
    }
    if (isSheetOpen) {
        VoicePresetPickerSheet(
            availablePresets = availablePresets,
            selectedPreset = selectedPreset,
            onPresetSelected = { preset ->
                onPresetSelected(preset)
                isSheetOpen = false
            },
            onDismiss = { isSheetOpen = false },
        )
    }
}
