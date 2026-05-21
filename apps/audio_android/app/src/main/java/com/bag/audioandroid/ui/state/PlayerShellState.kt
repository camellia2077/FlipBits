package com.bag.audioandroid.ui.state

enum class PlayerSurfaceValue {
    Collapsed,
    Expanded,
}

data class PlayerSurfaceState(
    val current: PlayerSurfaceValue,
    val target: PlayerSurfaceValue? = null,
    val dragOffsetPx: Float = 0f,
    val isDragging: Boolean = false,
    val animationProgress: Float = if (current == PlayerSurfaceValue.Expanded) 1f else 0f,
)

enum class QueueSheetValue {
    Hidden,
    Peek,
    Half,
    Expanded,
}

data class QueueSheetState(
    val current: QueueSheetValue,
    val target: QueueSheetValue? = null,
    val dragOffsetPx: Float = 0f,
    val isDragging: Boolean = false,
    val animationProgress: Float =
        when (current) {
            QueueSheetValue.Hidden -> 0f
            QueueSheetValue.Peek -> 0.33f
            QueueSheetValue.Half -> 0.66f
            QueueSheetValue.Expanded -> 1f
        },
)

data class PlayerShellState(
    val surface: PlayerSurfaceState = PlayerSurfaceState(current = PlayerSurfaceValue.Collapsed),
    val queue: QueueSheetState = QueueSheetState(current = QueueSheetValue.Hidden),
)

internal val PlayerShellState.isExpandedPlayerVisible: Boolean
    get() = surface.current == PlayerSurfaceValue.Expanded

internal val PlayerShellState.isQueueVisible: Boolean
    get() = queue.current != QueueSheetValue.Hidden

internal val PlayerShellState.isDockQueueVisible: Boolean
    get() = !isExpandedPlayerVisible && isQueueVisible

internal val PlayerShellState.isExpandedQueueVisible: Boolean
    get() = isExpandedPlayerVisible && isQueueVisible

sealed interface PlayerShellEvent {
    data object OpenExpandedPlayer : PlayerShellEvent

    data object CollapseExpandedPlayer : PlayerShellEvent

    data class OpenQueue(
        val preferExpandedSurface: Boolean,
    ) : PlayerShellEvent

    data object CloseQueue : PlayerShellEvent

    data class SetQueueValue(
        val value: QueueSheetValue,
        val preferExpandedSurface: Boolean = true,
    ) : PlayerShellEvent

    data class SelectQueueItem(
        val keepExpandedPlayer: Boolean = true,
    ) : PlayerShellEvent
}

internal fun PlayerShellState.reduce(event: PlayerShellEvent): PlayerShellState =
    when (event) {
        PlayerShellEvent.OpenExpandedPlayer ->
            copy(
                surface = PlayerSurfaceState(current = PlayerSurfaceValue.Expanded),
                queue = queue.copy(current = QueueSheetValue.Hidden, target = null, dragOffsetPx = 0f, isDragging = false),
            )

        PlayerShellEvent.CollapseExpandedPlayer ->
            PlayerShellState(
                surface = PlayerSurfaceState(current = PlayerSurfaceValue.Collapsed),
                queue = QueueSheetState(current = QueueSheetValue.Hidden),
            )

        is PlayerShellEvent.OpenQueue ->
            copy(
                surface =
                    if (event.preferExpandedSurface) {
                        PlayerSurfaceState(current = PlayerSurfaceValue.Expanded)
                    } else {
                        surface.copy(target = null, dragOffsetPx = 0f, isDragging = false)
                    },
                queue = QueueSheetState(current = QueueSheetValue.Half),
            )

        PlayerShellEvent.CloseQueue ->
            copy(
                queue = QueueSheetState(current = QueueSheetValue.Hidden),
            )

        is PlayerShellEvent.SetQueueValue ->
            copy(
                surface =
                    if (event.preferExpandedSurface) {
                        PlayerSurfaceState(current = PlayerSurfaceValue.Expanded)
                    } else {
                        surface.copy(target = null, dragOffsetPx = 0f, isDragging = false)
                    },
                queue = QueueSheetState(current = event.value),
            )

        is PlayerShellEvent.SelectQueueItem ->
            if (event.keepExpandedPlayer) {
                copy(
                    surface = PlayerSurfaceState(current = PlayerSurfaceValue.Expanded),
                    queue = QueueSheetState(current = QueueSheetValue.Hidden),
                )
            } else {
                PlayerShellState(
                    surface = PlayerSurfaceState(current = PlayerSurfaceValue.Collapsed),
                    queue = QueueSheetState(current = QueueSheetValue.Hidden),
                )
            }
    }
