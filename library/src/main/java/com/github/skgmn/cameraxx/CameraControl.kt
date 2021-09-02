package com.github.skgmn.cameraxx

import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalExposureCompensation
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import kotlinx.coroutines.guava.await

class CameraControl internal constructor(private val camera: Camera) {
    suspend fun enableTorch(torch: Boolean) {
        camera.cameraControl.enableTorch(torch).await()
    }

    suspend fun startFocusAndMetering(action: FocusMeteringAction): FocusMeteringResult {
        return camera.cameraControl.startFocusAndMetering(action).await()
    }

    suspend fun cancelFocusAndMetering() {
        camera.cameraControl.cancelFocusAndMetering().await()
    }

    @ExperimentalExposureCompensation
    suspend fun setExposureCompensationIndex(value: Int): Int {
        return camera.cameraControl.setExposureCompensationIndex(value).await()
    }

    suspend fun setLinearZoom(linearZoom: Float) {
        camera.cameraControl.setLinearZoom(linearZoom).await()
    }

    suspend fun setZoomRatio(ratio: Float) {
        camera.cameraControl.setZoomRatio(ratio).await()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraControl

        if (camera !== other.camera) return false

        return true
    }

    override fun hashCode(): Int {
        return camera.hashCode()
    }
}