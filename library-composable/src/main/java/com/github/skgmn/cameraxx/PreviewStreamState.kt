package com.github.skgmn.cameraxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

class PreviewStreamState {
    internal val isStreamingState = mutableStateOf(false)

    /**
     * true if preview is streaming.
     *
     * @see [androidx.camera.view.PreviewView.StreamState]
     */
    val isStreaming by isStreamingState
}