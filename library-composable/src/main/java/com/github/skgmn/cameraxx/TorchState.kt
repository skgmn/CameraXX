package com.github.skgmn.cameraxx

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.*

class TorchState internal constructor() {
    internal val _hasFlashUnit = MutableStateFlow<Boolean?>(null)

    val isOn = MutableStateFlow<Boolean?>(null)
    val hasFlashUnit: StateFlow<Boolean?>
        get() = _hasFlashUnit
}

@Composable
fun rememberTorchState(): TorchState = remember { TorchState() }