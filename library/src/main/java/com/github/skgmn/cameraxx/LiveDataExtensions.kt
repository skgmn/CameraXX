package com.github.skgmn.cameraxx

import androidx.lifecycle.LiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> LiveData<T>.toFlow(): Flow<T> {
    return callbackFlow {
        value?.let {
            trySend(it)
            if (!isActive) {
                return@callbackFlow
            }
        }
        val observer: (T) -> Unit = {
            trySend(it)
        }
        observeForever(observer)
        awaitClose {
            removeObserver(observer)
        }
    }
}