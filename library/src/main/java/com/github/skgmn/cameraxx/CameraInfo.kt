package com.github.skgmn.cameraxx

import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalExposureCompensation
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import com.github.skgmn.coroutineskit.lifecycle.toStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A wrapper class of [androidx.camera.core.CameraInfo] to use methods as
 * coroutines and flows.
 */
class CameraInfo internal constructor(private val camera: Camera) {
    /**
     * @see [androidx.camera.core.CameraInfo.getSensorRotationDegrees]
     */
    val sensorRotationDegrees: Int
        get() = camera.cameraInfo.sensorRotationDegrees

    /**
     * @see [androidx.camera.core.CameraInfo.hasFlashUnit]
     */
    val hasFlashUnit: Boolean
        get() = camera.cameraInfo.hasFlashUnit()

    /**
     * @see [androidx.camera.core.CameraInfo.getExposureState]
     */
    val exposureState: ExposureState
        @ExperimentalExposureCompensation
        get() = camera.cameraInfo.exposureState

    /**
     * @see [androidx.camera.core.CameraInfo.getSensorRotationDegrees]
     */
    fun getSensorRotationDegrees(relativeRotation: Int): Int {
        return camera.cameraInfo.getSensorRotationDegrees(relativeRotation)
    }

    /**
     * @see [androidx.camera.core.CameraInfo.getTorchState]
     */
    fun getTorchState(): StateFlow<Int?> {
        return camera.cameraInfo.torchState.toStateFlow()
    }

    /**
     * @see [androidx.camera.core.CameraInfo.getZoomState]
     */
    fun getZoomState(): StateFlow<ZoomState?> {
        return camera.cameraInfo.zoomState.toStateFlow()
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