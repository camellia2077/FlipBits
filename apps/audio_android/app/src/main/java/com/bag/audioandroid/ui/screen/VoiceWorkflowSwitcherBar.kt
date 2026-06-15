package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption

@Composable
internal fun VoiceWorkflowSwitcherBar(
    selectedWorkflowMode: VoiceWorkflowModeOption,
    onWorkflowModeSelected: (VoiceWorkflowModeOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        VoiceWorkflowSelectorSection(
            selectedWorkflowMode = selectedWorkflowMode,
            onWorkflowModeSelected = onWorkflowModeSelected,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
        )
    }
}
