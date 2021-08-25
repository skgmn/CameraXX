package com.github.skgmn.cameraxx

import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalExposureCompensation
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import kotlinx.coroutines.flow.Flow

class CameraInfo internal constructor(private val camera: Camera) {
    val sensorRotationDegrees: Int
        get() = camera.cameraInfo.sensorRotationDegrees
    val hasFlashUnit: Boolean
        get() = camera.cameraInfo.hasFlashUnit()
    val exposureState: ExposureState
        @ExperimentalExposureCompensation
        get() = camera.cameraInfo.exposureState

    fun getSensorRotationDegrees(relativeRotation: Int): Int {
        return camera.cameraInfo.getSensorRotationDegrees(relativeRotation)
    }

    fun getTorchState(): Flow<Int> {
        return camera.cameraInfo.torchState.toFlow()
    }

    fun getZoomState(): Flow<ZoomState> {
        return camera.cameraInfo.zoomState.toFlow()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraInfo

        if (camera !== other.camera) return false

        return true
    }

    override fun hashCode(): Int {
        return camera.hashCode()
    }
}