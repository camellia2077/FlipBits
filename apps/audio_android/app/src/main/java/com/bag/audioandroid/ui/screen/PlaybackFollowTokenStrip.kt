package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData

@Composable
internal fun PlaybackFollowTokenStrip(
    followData: PayloadFollowViewData,
    presentationState: PlaybackFollowPresentationState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(presentationState.activeTextIndex) {
        if (presentationState.activeTextIndex >= 0) {
            listState.animateScrollToItem(presentationState.activeTextIndex)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = PlaybackLyricsHorizontalPadding, vertical = 12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 188.dp, max = 240.dp)
                .testTag("follow-token-strip"),
    ) {
        itemsIndexed(followData.textTokens) { index, token ->
            PlaybackFollowTokenCard(
                token = token,
                rawDisplayUnits = presentationState.rawDisplayUnitsByToken[index].orEmpty(),
                annotationMode = presentationState.followViewMode,
                isActive = index == presentationState.activeTextIndex,
                activeByteIndexWithinToken =
                    if (index == presentationState.activeTextIndex) {
                        presentationState.activeByteIndexWithinToken
                    } else {
                        -1
                    },
            )
        }
    }
}
