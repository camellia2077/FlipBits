package com.bag.audioandroid.ui.screen

import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bag.audioandroid.ui.model.ThemeStyleOption

@Composable
internal fun VoiceSectionContainer(
    selectedThemeStyle: ThemeStyleOption,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (selectedThemeStyle == ThemeStyleOption.FactionTheme) {
        AudioSectionContainer(
            modifier = modifier,
            content = content,
        )
    } else {
        ElevatedCard(
            modifier = modifier,
        ) {
            content()
        }
    }
}
