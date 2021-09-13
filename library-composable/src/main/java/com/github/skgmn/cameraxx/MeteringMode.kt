package com.github.skgmn.cameraxx

import androidx.camera.core.FocusMeteringAction

/**
 * Focus/Metering mode used to specify which 3A regions is activated for corresponding
 * MeteringPoint.
 */
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

        /**
         * A flag used in metering mode indicating the AF (Auto Focus) region is enabled. An
         * autofocus scan is also triggered when FLAG_AF is assigned.
         */
        val AutoFocus = MeteringMode(FocusMeteringAction.FLAG_AF)

        /**
         * A flag used in metering mode indicating the AE (Auto Exposure) region is enabled.
         */
        val AutoExposure = MeteringMode(FocusMeteringAction.FLAG_AE)

        /**
         * A flag used in metering mode indicating the AWB (Auto White Balance) region is enabled.
         */
        val AutoWhiteBalance = MeteringMode(FocusMeteringAction.FLAG_AWB)
    }
}