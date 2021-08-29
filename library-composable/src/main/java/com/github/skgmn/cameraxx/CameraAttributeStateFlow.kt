package com.github.skgmn.cameraxx

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class CameraAttributeStateFlow<T : Any>(
    private val source: MutableStateFlow<CameraAttribute<T>?>
) : MutableStateFlow<T?> {
    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T?>) {
        source.map { it?.value }.distinctUntilChanged().collect(collector)
    }

    override val subscriptionCount: StateFlow<Int>
        get() = source.subscriptionCount

    override suspend fun emit(value: T?) {
        requireNotNull(value)
        source.emit(CameraAttribute(value, false))
    }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        source.resetReplayCache()
    }

    override fun tryEmit(value: T?): Boolean {
        requireNotNull(value)
        return source.tryEmit(CameraAttribute(value, false))
    }

    override var value: T?
        get() = source.value?.value
        set(value) {
            requireNotNull(value)
            source.value = CameraAttribute(value, false)
        }

    override fun compareAndSet(expect: T?, update: T?): Boolean {
        requireNotNull(update)
        while (true) {
            val oldAttribute = source.value
            if (oldAttribute?.value != expect) {
                return false
            }
            if (source.compareAndSet(oldAttribute, CameraAttribute(update, false))) {
                return true
            }
        }
    }

    override val replayCache: List<T?>
        get() = source.replayCache.map { it?.value }
}