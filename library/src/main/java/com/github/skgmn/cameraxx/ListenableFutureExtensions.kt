package com.github.skgmn.cameraxx

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal suspend fun <V> ListenableFuture<V>.await(cancellable: Boolean = true): V {
    if (cancellable) {
        return suspendCancellableCoroutine { cont ->
            val listener = {
                if (!isCancelled) {
                    cont.resume(get())
                }
            }
            addListener(listener, ImmediateExecutor())
            cont.invokeOnCancellation {
                cancel(false)
            }
        }
    } else {
        return suspendCoroutine { cont ->
            val listener = {
                if (!isCancelled) {
                    cont.resume(get())
                }
            }
            addListener(listener, ImmediateExecutor())
        }
    }
}