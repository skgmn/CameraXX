package com.github.skgmn.cameraxx

import androidx.camera.core.Camera

/**
 * A wrapper class of [androidx.camera.core.Camera] to use methods as coroutines.
 *
 * An instance of this class can be retrieved by `app:onCameraRetrieved` xml attribute
 * when it's used with `cameraxx-bindingadapter` module.
 *
 * If you are using `cameraxx-composable` module for Jetpack Compose,
 * this class is only used inside `CameraPreview` and not exposed publicly.
 */
open class Camera(private val camera: Camera) {
    val cameraControl by lazy { CameraControl(camera) }
    val cameraInfo by lazy { CameraInfo(camera) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as com.github.skgmn.cameraxx.Camera

        if (camera !== other.camera) return false

        return true
    }

    override fun hashCode(): Int {
        return camera.hashCode()
    }
}