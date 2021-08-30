package com.github.skgmn.cameraxx

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FocusMeteringState {
    internal val progressFlow = MutableStateFlow(FocusMeteringProgress.Idle)

    val meteringParameters = MutableStateFlow(FocusMeteringParameters())
    val meteringPoints = MutableStateFlow(TapPoint)
    val progress: StateFlow<FocusMeteringProgress> get() = progressFlow

    companion object {
        val TapPoint = listOf(Offset.Unspecified)
    }
}