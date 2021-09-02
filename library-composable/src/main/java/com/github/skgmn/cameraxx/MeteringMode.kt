package com.github.skgmn.cameraxx

import androidx.camera.core.FocusMeteringAction

@JvmInline
value class MeteringMode(internal val value: Int) {
    operator fun plus(other: MeteringMode): MeteringMode {
        return MeteringMode(value or other.value)
    }

    operator fun contains(other: MeteringMode): Boolean {
        return (value and other.value) != 0
    }

    companion object {
        val None = MeteringMode(0)
        val AutoFocus = MeteringMode(FocusMeteringAction.FLAG_AF)
        val AutoExposure = MeteringMode(FocusMeteringAction.FLAG_AE)
        val AutoWhiteBalance = MeteringMode(FocusMeteringAction.FLAG_AWB)
    }
}