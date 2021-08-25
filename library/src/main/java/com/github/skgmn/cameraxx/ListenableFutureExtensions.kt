package com.github.skgmn.cameraxx

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun <V> ListenableFuture<V>.await(): V {
    return suspendCancellableCoroutine { cont ->
        val listener = {
            cont.resume(get())
        }
        addListener(listener, ImmediateExecutor())
        cont.invokeOnCancellation {
            cancel(false)
        }
    }
}