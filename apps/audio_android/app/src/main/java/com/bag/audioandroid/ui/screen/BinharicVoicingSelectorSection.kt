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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption

@Composable
internal fun BinharicVoicingSelectorSection(
    enabled: Boolean,
    selectedStyle: VoiceFxSubvoiceStyleOption,
    onStyleSelected: (VoiceFxSubvoiceStyleOption) -> Unit,
) {
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceSettingPickerRow(
            title = stringResource(R.string.voice_voicing_style_title),
            value = stringResource(selectedStyle.labelResId),
            contentDescription = stringResource(R.string.voice_action_select_voicing_style),
            testTag = "binharic-voicing-style-selector",
            enabled = enabled,
            onClick = { isSheetOpen = true },
        )
        Text(
            text = stringResource(selectedStyle.descriptionResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("binharic-voicing-style-description"),
        )
    }

    if (isSheetOpen) {
        BinharicVoicingPickerSheet(
            selectedStyle = selectedStyle,
            onStyleSelected = { option ->
                onStyleSelected(option)
                isSheetOpen = false
            },
            onDismiss = { isSheetOpen = false },
        )
    }
}
