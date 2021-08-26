package com.github.skgmn.cameraxx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ZoomState {
    internal val ratioRangeFlow = MutableStateFlow<ClosedRange<Float>?>(null)
    internal val pinchZoomInProgressFlow = MutableStateFlow(false)
    internal val ratioFlow = MutableStateFlow<CameraAttribute<Float>?>(null)

    val ratioRange: StateFlow<ClosedRange<Float>?>
        get() = ratioRangeFlow
    val ratio: MutableStateFlow<Float?> = CameraAttributeStateFlow(ratioFlow)
    val pinchZoomInProgress: StateFlow<Boolean>
        get() = pinchZoomInProgressFlow
}