package com.github.skgmn.cameraxx

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class FocusMeteringParameters(
    val meteringMode: MeteringMode =
        MeteringMode.AutoFocus + MeteringMode.AutoExposure + MeteringMode.AutoWhiteBalance,
    val autoCancelDuration: Duration? = Duration.seconds(5)
)