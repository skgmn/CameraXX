package com.github.skgmn.cameraxx

import androidx.annotation.GuardedBy
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageProxyWrapper
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import java.lang.ref.WeakReference
import java.util.*
import kotlin.coroutines.CoroutineContext

@GuardedBy("analysisDispatchers")
private val analysisDispatchers =
    WeakHashMap<ImageAnalysis, WeakReference<ImageAnalysisDispatcher>>()

@Deprecated("Use analyze<T>((ImageProxy) -> T) instead.")
@OptIn(ExperimentalCoroutinesApi::class)
fun ImageAnalysis.analyze(): Flow<ImageProxy> {
    return callbackFlow {
        val imageProxies = Collections.newSetFromMap(WeakHashMap<ImageProxy, Boolean>())
        setAnalyzer(MoreExecutors.directExecutor(), { imageProxy ->
            val imageProxyWrapper = ImageProxyWrapper.wrap(imageProxy)
            synchronized(imageProxies) {
                imageProxies += imageProxyWrapper
            }
            ImageProxyWrapper.addOnCloseListener(imageProxyWrapper) {
                synchronized(imageProxies) {
                    imageProxies -= imageProxyWrapper
                }
            }
            if (!trySend(imageProxyWrapper).isSuccess) {
                synchronized(imageProxies) {
                    imageProxies -= imageProxyWrapper
                }
                imageProxyWrapper.close()
            }
        })
        awaitClose {
            val proxies = synchronized(imageProxies) {
                imageProxies.toList().also { imageProxies.clear() }
            }
            proxies.forEach { it.close() }
            clearAnalyzer()
        }
    }
}

/**
 * Analyze each camera frames.
 *
 * It is vital to provide [transform] lambda to manage the lifecycle of [ImageProxy].
 * [ImageProxy] is only valid while all [transform] lambdas are running.
 * It is not needed to call [ImageProxy.close] in [transform] or any subsequent code flow.
 */
@OptIn(InternalCoroutinesApi::class)
fun <T> ImageAnalysis.analyze(transform: suspend (ImageProxy) -> T): Flow<T> {
    @Suppress("UNCHECKED_CAST")
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            coroutineScope {
                val entry = ImageAnalysisDispatcher.Entry(
                    transform,
                    collector as FlowCollector<Any?>,
                    coroutineContext
                )
                val dispatcher = ImageAnalysisDispatcher.get(this@analyze, entry)
                try {
                    while (isActive) {
                        delay(Long.MAX_VALUE)
                    }
                } finally {
                    dispatcher -= entry
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private class ImageAnalysisDispatcher(
    private val imageAnalysis: ImageAnalysis,
    firstEntry: Entry
) {
    @GuardedBy("collectors")
    private val entries = mutableListOf(firstEntry)

    private val channel = Channel<ImageProxy>(1, BufferOverflow.SUSPEND) {
        it.use {}
    }
    private val job = GlobalScope.launch {
        for (imageProxy in channel) {
            val entries = synchronized(entries) { entries.toList() }
            val transformedList = imageProxy.use { proxy ->
                supervisorScope {
                    val deferredList = entries.map { entry ->
                        async(entry.context) { entry.transform(proxy) }
                    }
                    deferredList.map { it.await() }
                }
            }
            for (i in entries.indices) {
                val transformed = transformedList[i]
                val entry = entries[i]
                launch(entry.context) { entry.collector.emit(transformed) }
            }
        }
    }

    init {
        imageAnalysis.setAnalyzer(MoreExecutors.directExecutor()) {
            if (!channel.trySend(it).isSuccess) {
                it.use {}
            }
        }
    }

    operator fun minusAssign(entry: Entry) {
        synchronized(entries) {
            entries -= entry
            if (entries.isEmpty()) {
                synchronized(analysisDispatchers) {
                    analysisDispatchers -= imageAnalysis
                }
                imageAnalysis.clearAnalyzer()
                channel.close()
                job.cancel()
            }
        }
    }

    class Entry(
        val transform: suspend (ImageProxy) -> Any?,
        val collector: FlowCollector<Any?>,
        val context: CoroutineContext
    )

    companion object {
        fun get(imageAnalysis: ImageAnalysis, entry: Entry): ImageAnalysisDispatcher {
            return synchronized(analysisDispatchers) {
                analysisDispatchers[imageAnalysis]?.get()?.apply {
                    synchronized(entries) {
                        entries += entry
                    }
                } ?: ImageAnalysisDispatcher(imageAnalysis, entry).also {
                    analysisDispatchers[imageAnalysis] = WeakReference(it)
                }
            }
        }
    }
}