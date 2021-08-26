package com.github.skgmn.cameraxx

import kotlinx.coroutines.flow.*

class TorchState {
    internal val hasFlashUnitFlow = MutableStateFlow<Boolean?>(null)

    val isOn = MutableStateFlow<Boolean?>(null)
    val hasFlashUnit: StateFlow<Boolean?>
        get() = hasFlashUnitFlow
}