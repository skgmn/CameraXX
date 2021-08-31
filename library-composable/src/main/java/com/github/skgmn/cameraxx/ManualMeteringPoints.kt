package com.github.skgmn.cameraxx

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ManualMeteringPoints(
    val offsets: MutableStateFlow<List<Offset>>
) : MeteringPoints() {
    constructor() : this(MutableStateFlow(emptyList()))

    override fun getOffsets(): Flow<List<Offset>> {
        return offsets
    }
}