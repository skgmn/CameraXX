package com.github.skgmn.cameraxx

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class TapMeteringPoints : MeteringPoints() {
    internal val tapOffsetFlow = MutableStateFlow<Offset?>(null)

    val tapOffset: StateFlow<Offset?>
        get() = tapOffsetFlow

    override fun getOffsets(): Flow<List<Offset>> {
        return tapOffsetFlow.map { offset ->
            offset?.let { listOf(it) } ?: emptyList()
        }
    }
}