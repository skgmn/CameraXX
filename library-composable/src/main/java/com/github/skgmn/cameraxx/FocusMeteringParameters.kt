package com.github.skgmn.cameraxx

data class FocusMeteringParameters(
    val meteringMode: MeteringMode =
        MeteringMode.AutoFocus + MeteringMode.AutoExposure + MeteringMode.AutoWhiteBalance,
    val autoCancelDurationMs: Long? = 5000L
)