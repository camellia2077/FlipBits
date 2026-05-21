package com.bag.audioandroid.ui.screen

internal fun flashVisualPrimitiveEstimate(
    mode: FlashSignalVisualizationMode,
    drawableSegments: Int,
    buckets: Int,
    hasFixedTimeline: Boolean,
): Int =
    if (hasFixedTimeline) {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> drawableSegments * 2
            FlashSignalVisualizationMode.Pitch -> drawableSegments
            FlashSignalVisualizationMode.Hz -> drawableSegments
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    } else {
        when (mode) {
            FlashSignalVisualizationMode.Lanes -> buckets * 2
            FlashSignalVisualizationMode.Pitch -> buckets * 2
            FlashSignalVisualizationMode.Hz -> buckets
            FlashSignalVisualizationMode.Pulse -> FlashPulseVisibleCellCount
        }
    }
