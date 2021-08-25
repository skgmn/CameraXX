package com.github.skgmn.cameraxx

import androidx.camera.core.Camera

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