package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ZoomState(
    pinchZoomEnabled: Boolean = false
) {
    internal val ratioRangeState = mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
    internal val pinchZoomInProgressState = mutableStateOf(false)

    /**
     * A range of zoom ratio which the camera supports.
     * When [ratio] is being set, caller MUST ensure the value is between this range.
     * Ignore until it becomes non-null.
     */
    val ratioRange by ratioRangeState
    /**
     * A zoom ratio of the camera.
     * Ignore until it becomes non-null.
     */
    var ratio by mutableStateOf<Float?>(null)
    var pinchZoomEnabled by mutableStateOf(pinchZoomEnabled)
    val pinchZoomInProgress by pinchZoomInProgressState
}