package com.bag.audioandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private object PlayerScaffoldDefaults {
    val dockHorizontalPadding = 12.dp
    val bottomNavigationBarHeight = 80.dp
    val miniPlayerHeight = 72.dp
    val dockSectionSpacing = 8.dp
    val contentBottomBreath = 16.dp
    val snackbarBottomSpacing = 12.dp
}

/**
 * The scaffold uses a bottom-aligned dock layered over the content viewport so the mini-player
 * can stay visible after encode completes. Content gets matching bottom padding and can scroll
 * behind the dock without losing access to its last items.
 */
@Composable
internal fun PlayerScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: (@Composable () -> Unit)? = null,
    miniPlayer: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val bottomNavigationHeight = PlayerScaffoldDefaults.bottomNavigationBarHeight
    val miniPlayerHeight = if (miniPlayer != null) PlayerScaffoldDefaults.miniPlayerHeight else 0.dp
    val dockSpacing = if (miniPlayer != null) PlayerScaffoldDefaults.dockSectionSpacing else 0.dp
    val contentBottomPadding =
        bottomNavigationHeight + miniPlayerHeight + dockSpacing + PlayerScaffoldDefaults.contentBottomBreath
    val snackbarBottomPadding =
        bottomNavigationHeight + miniPlayerHeight + dockSpacing + PlayerScaffoldDefaults.snackbarBottomSpacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(
                PaddingValues(bottom = contentBottomPadding),
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                miniPlayer?.let { miniPlayerContent ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerScaffoldDefaults.dockHorizontalPadding),
                    ) {
                        miniPlayerContent()
                    }
                    Spacer(modifier = Modifier.height(PlayerScaffoldDefaults.dockSectionSpacing))
                }

                bottomBar()
            }

            snackbarHost?.let { host ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = snackbarBottomPadding,
                            ),
                ) {
                    host()
                }
            }
        }
    }
}
