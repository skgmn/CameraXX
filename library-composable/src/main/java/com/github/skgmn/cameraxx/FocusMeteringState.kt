package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FocusMeteringState(
    parameters: FocusMeteringParameters = FocusMeteringParameters()
) {
    internal val progressState = mutableStateOf(FocusMeteringProgress.Idle)

    var parameters by mutableStateOf(parameters)
    var meteringPoints by mutableStateOf<MeteringPoints>(TapMeteringPoints())
    val progress by progressState
}