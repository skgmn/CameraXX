package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ZoomState(
    pinchZoomEnabled: Boolean = false
) {
    internal val ratioRangeState = mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
    internal val pinchZoomInProgressState = mutableStateOf(false)

    val ratioRange by ratioRangeState
    var ratio by mutableStateOf<Float?>(null)
    var pinchZoomEnabled by mutableStateOf(pinchZoomEnabled)
    val pinchZoomInProgress by pinchZoomInProgressState
}