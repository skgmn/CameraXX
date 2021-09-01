package com.github.skgmn.cameraxx

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class TapMeteringPoints : MeteringPoints() {
    internal val tapOffsetState = mutableStateOf<Offset?>(null)

    val tapOffset by tapOffsetState

    override fun getOffsetListState(): State<List<Offset>> {
        return derivedStateOf {
            tapOffsetState.value?.let { listOf(it) } ?: emptyList()
        }
    }
}