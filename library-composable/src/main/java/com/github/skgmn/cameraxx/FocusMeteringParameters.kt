package com.github.skgmn.cameraxx

data class FocusMeteringParameters(
    /**
     * Metering mode is a combination of flags consisting of [MeteringMode.AutoFocus],
     * [MeteringMode.AutoExposure], and [MeteringMode.AutoWhiteBalance]. This combination indicates
     * whether the MeteringPoint is used to set AF(Auto Focus) region, AE(Auto Exposure) region or
     * AWB(Auto White Balance) region.
     */
    val meteringMode: MeteringMode =
        MeteringMode.AutoFocus + MeteringMode.AutoExposure + MeteringMode.AutoWhiteBalance,

    /**
     * Sets the auto-cancel duration. By default, auto-cancel is enabled with 5 seconds duration.
     * The duration must be greater than or equal to 1 otherwise it will throw a
     * IllegalArgumentException.
     * Pass null to disable auto-cancel.
     */
    val autoCancelDurationMs: Long? = 5000L
)