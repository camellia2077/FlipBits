package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
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
                    val isSelected = transportMode == option
                    Text(
                        text = stringResource(option.labelResId),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    }
}
