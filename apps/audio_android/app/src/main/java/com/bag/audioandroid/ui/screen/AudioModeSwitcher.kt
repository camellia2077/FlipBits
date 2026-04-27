package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.TransportModeOption

@Composable
internal fun AudioModeSwitcher(
    transportMode: TransportModeOption,
    onTransportModeSelected: (TransportModeOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        TransportModeOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = transportMode == option,
                onClick = { onTransportModeSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TransportModeOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                modifier = Modifier.weight(1f),
                label = {
                    Text(stringResource(option.labelResId))
                },
            )
        }
    }
}
