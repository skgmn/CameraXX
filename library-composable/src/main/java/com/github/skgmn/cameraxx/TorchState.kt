package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TorchState {
    internal val isOnState = mutableStateOf<Boolean?>(null)
    internal val hasFlashUnitState = mutableStateOf<Boolean?>(null)

    /**
     * An on/off state of the torch of the camera.
     * Ignore until it becomes non-null.
     */
    var isOn by isOnState
    val hasFlashUnit by hasFlashUnitState
}