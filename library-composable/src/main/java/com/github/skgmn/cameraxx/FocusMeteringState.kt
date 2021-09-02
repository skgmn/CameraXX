package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FocusMeteringState(
    parameters: FocusMeteringParameters = FocusMeteringParameters(),
    meteringPoints: MeteringPoints = TapMeteringPoints()
) {
    internal val progressState = mutableStateOf(FocusMeteringProgress.Idle)

    var parameters by mutableStateOf(parameters)
    var meteringPoints by mutableStateOf(meteringPoints)
    val progress by progressState
}