package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FocusMeteringState {
    internal val progressState = mutableStateOf(FocusMeteringProgress.Idle)

    var meteringParameters by mutableStateOf(FocusMeteringParameters())
    var meteringPoints by mutableStateOf<MeteringPoints>(TapMeteringPoints())
    val progress by progressState
}