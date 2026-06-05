package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData

@Composable
internal fun ProEncodingExplanationVisualizer(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
    modifier: Modifier = Modifier,
) {
    val state = rememberProEncodingVisualizationState(followData, displayedSamples, frameSamples)
    if (state == null) {
        Text(
            text = stringResource(R.string.audio_pro_visual_waiting_follow),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("pro-encoding-visualizer-empty"),
        )
        return
    }

    Column(
        modifier =
            modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .testTag("pro-encoding-visualizer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProFrequencyHitMap(
            symbols = state.symbols,
            modifier = Modifier.fillMaxWidth(),
        )
        ProPlaybackGuideCard(
            currentSymbol = state.currentSymbol,
            nextSymbol = state.nextSymbol,
            tokenByteMapping = state.tokenByteMapping,
            modifier = Modifier.fillMaxWidth(),
        )
        ProSymbolNibbleStrip(
            symbols = state.symbols,
            currentSymbol = state.currentSymbol,
            modifier = Modifier.fillMaxWidth(),
        )
        ProByteExplanationCard(
            explanation = state.byteExplanation,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun rememberProEncodingVisualizationState(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
): ProEncodingVisualizationState? {
    val analysisCache = remember(followData, frameSamples) { ProEncodingVisualizationAnalysisCache() }
    return remember(followData, displayedSamples, frameSamples) {
        analysisCache.state(displayedSamples) {
            deriveProEncodingVisualizationState(
                followData = followData,
                displayedSamples = displayedSamples,
                frameSamples = frameSamples,
            )
        }
    }
}

internal class ProEncodingVisualizationAnalysisCache {
    private val statesByDisplayedSamples = LinkedHashMap<Int, ProEncodingVisualizationState?>()

    fun state(
        displayedSamples: Int,
        build: () -> ProEncodingVisualizationState?,
    ): ProEncodingVisualizationState? {
        if (statesByDisplayedSamples.containsKey(displayedSamples)) {
            return statesByDisplayedSamples[displayedSamples]
        }
        val state = build()
        statesByDisplayedSamples[displayedSamples] = state
        if (statesByDisplayedSamples.size > ProEncodingVisualizationAnalysisCacheMaxEntries) {
            val eldestKey = statesByDisplayedSamples.keys.first()
            statesByDisplayedSamples.remove(eldestKey)
        }
        return state
    }
}

private const val ProEncodingVisualizationAnalysisCacheMaxEntries = 16
