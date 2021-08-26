package com.github.skgmn.cameraxx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.*

class ZoomState internal constructor() {
    internal var _ratioRange = MutableStateFlow<ClosedRange<Float>?>(null)
    internal val _ratio = MutableStateFlow<Float?>(null)
    internal val _pinchZoomInProgress = MutableStateFlow(false)

    internal val cameraFlow = MutableStateFlow<Camera?>(null)

    val ratioRange: StateFlow<ClosedRange<Float>?>
        get() = _ratioRange
    val ratio: StateFlow<Float?>
        get() = _ratio
    val pinchZoomInProgress: StateFlow<Boolean>
        get() = _pinchZoomInProgress

    suspend fun requestRatio(ratio: Float) {
        combine(
            cameraFlow.filterNotNull(),
            _ratioRange.filterNotNull()
        ) { camera, ratioRange ->
            // I don't know why but compiler cannot find coerceIn(ClosedRange<Float>)
            val coercedRatio = ratio.coerceIn(ratioRange.start, ratioRange.endInclusive)
            suspend { camera.cameraControl.setZoomRatio(coercedRatio) }
        }.first()()
    }
}

@Composable
fun rememberZoomState(): ZoomState = remember { ZoomState() }