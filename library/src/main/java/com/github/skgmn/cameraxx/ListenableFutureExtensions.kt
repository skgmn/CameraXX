package com.github.skgmn.cameraxx

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal suspend fun <V> ListenableFuture<V>.await(cancellable: Boolean = true): V {
    if (cancellable) {
        return suspendCancellableCoroutine { cont ->
            val listener = {
                if (!isCancelled) {
                    try {
                        cont.resume(get())
                    } catch (e: Throwable) {
                        cont.resumeWithException(unwrapException(e))
                    }
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
                    try {
                        cont.resume(get())
                    } catch (e: Throwable) {
                        cont.resumeWithException(unwrapException(e))
                    }
                }
            }
            addListener(listener, ImmediateExecutor())
        }
    }
}

private fun unwrapException(e: Throwable): Throwable {
    return if (e is ExecutionException) {
        e.cause ?: e
    } else {
        e
    }
}