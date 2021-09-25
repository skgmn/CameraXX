package com.github.skgmn.cameraxx

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

internal fun <T : Any> LiveData<T>.toStateFlow(): StateFlow<T?> {
    val stateFlow = MutableStateFlow(value)
    val observer: (T) -> Unit = { stateFlow.value = it }
    val refCount = AtomicInteger()
    val flow = stateFlow
        .onSubscription {
            if (refCount.getAndIncrement() == 0) {
                observeForever(observer)
            }
        }
        .onCompletion {
            if (refCount.decrementAndGet() == 0) {
                removeObserver(observer)
            }
        }
        .flowOn(Dispatchers.Main.immediate)
    return object : StateFlow<T?>, Flow<T?> by flow {
        override val replayCache: List<T?>
            get() = stateFlow.replayCache
        override val value: T?
            get() = stateFlow.value
    }
}