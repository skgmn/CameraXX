package com.github.skgmn.cameraxx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FocusMeteringState {
    internal val progressFlow = MutableStateFlow(FocusMeteringProgress.Idle)

    val meteringParameters = MutableStateFlow(FocusMeteringParameters())
    val meteringPoints = MutableStateFlow<MeteringPoints>(TapMeteringPoints())
    val progress: StateFlow<FocusMeteringProgress> get() = progressFlow
}