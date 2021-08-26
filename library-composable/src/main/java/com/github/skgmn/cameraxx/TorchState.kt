package com.github.skgmn.cameraxx

import kotlinx.coroutines.flow.*

class TorchState {
    internal val isOnFlow = MutableStateFlow<CameraAttribute<Boolean>?>(null)
    internal val hasFlashUnitFlow = MutableStateFlow<Boolean?>(null)

    val isOn: MutableStateFlow<Boolean?> = CameraAttributeStateFlow(isOnFlow)
    val hasFlashUnit: StateFlow<Boolean?>
        get() = hasFlashUnitFlow
}