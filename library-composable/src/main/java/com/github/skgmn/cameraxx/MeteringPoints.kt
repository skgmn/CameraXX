package com.github.skgmn.cameraxx

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed class MeteringPoints {
    internal abstract fun getOffsets(): Flow<List<Offset>>

    companion object {
        object Empty : MeteringPoints() {
            override fun getOffsets(): Flow<List<Offset>> {
                return flowOf(emptyList())
            }
        }
    }
}