package com.bag.audioandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.screen.MiniPlayerBar
import com.bag.audioandroid.ui.state.AudioAppUiState

private val AudioAndroidNavigationTabs = listOf(AppTab.Config, AppTab.Audio, AppTab.Library)

@Composable
internal fun AudioAndroidBottomBar(
    uiState: AudioAppUiState,
    navigationBarColors: androidx.compose.material3.NavigationBarItemColors,
    onTabSelected: (AppTab) -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenSavedAudioSheet: () -> Unit,
    onOpenPlayerDetailSheet: () -> Unit,
) {
    Column {
        uiState.miniPlayerModel?.let { model ->
            MiniPlayerBar(
                model = model,
                isPlaying = uiState.currentPlayback.isPlaying,
                onTogglePlayback = onTogglePlayback,
                onOpenSavedAudioSheet = onOpenSavedAudioSheet,
                onOpenDetails = onOpenPlayerDetailSheet,
                containerColor =
                    if (
                        uiState.selectedThemeStyle == ThemeStyleOption.BrandDualTone &&
                        uiState.selectedBrandTheme.id == "black_crimson_rite"
                    ) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        NavigationBar(
            modifier = Modifier.height(64.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            AudioAndroidNavigationTabs.forEach { tab ->
                val selected = tab == uiState.selectedTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelResId),
                        )
                    },
                    label = { Text(stringResource(tab.labelResId)) },
                    colors = navigationBarColors,
                )
            }
        }
    }
}
