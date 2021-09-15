package com.github.skgmn.cameraxx

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ManualMeteringPoints(
    initialOffsets: List<Offset>
) : MeteringPoints() {
    constructor() : this(emptyList())

    private val offsetListState = mutableStateOf(initialOffsets)

    /**
     * Offsets to focus on. They are [CameraPreview]'s layout coordinates.
     */
    var offsets by offsetListState

    override fun getOffsetListState(): State<List<Offset>> {
        return offsetListState
    }
}