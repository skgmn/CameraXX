package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ZoomState {
    internal val ratioRangeState = mutableStateOf<ClosedRange<Float>?>(null)
    internal val pinchZoomInProgressState = mutableStateOf(false)
    internal val ratioState = mutableStateOf<Float?>(null)

    val ratioRange by ratioRangeState
    var ratio by ratioState
    val pinchZoomInProgress by pinchZoomInProgressState
}