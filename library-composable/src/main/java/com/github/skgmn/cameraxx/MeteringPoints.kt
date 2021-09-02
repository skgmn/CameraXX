package com.github.skgmn.cameraxx

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed class MeteringPoints {
    internal abstract fun getOffsetListState(): State<List<Offset>>

    companion object {
        object Empty : MeteringPoints() {
            override fun getOffsetListState(): State<List<Offset>> {
                return mutableStateOf(emptyList())
            }
        }
    }
}