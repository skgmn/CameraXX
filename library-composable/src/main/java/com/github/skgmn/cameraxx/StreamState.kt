package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

class StreamState {
    internal val isStreamingState = mutableStateOf(false)

    val isStreaming by isStreamingState
}