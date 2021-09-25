package com.github.skgmn.cameraxx

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.guava.await
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get a singleton [ProcessCameraProvider].
 */
@Suppress("UnstableApiUsage")
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(this)
    return Futures.nonCancellationPropagating(future).await()
}

/**
 * Listen to [PreviewView.StreamState] of this [PreviewView].
 */
fun PreviewView.listenPreviewStreamState(): StateFlow<PreviewView.StreamState?> {
    return previewStreamState.toStateFlow()
}

/**
 * Take a picture and get a [ImageProxy] instance.
 */
suspend fun ImageCapture.takePicture(): ImageProxy {
    return suspendCoroutine { cont ->
        takePicture(
            MoreExecutors.directExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    cont.resume(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            })
    }
}

/**
 * Take a picture and save it to a file.
 */
suspend fun ImageCapture.takePicture(
    outputFileOptions: ImageCapture.OutputFileOptions
): ImageCapture.OutputFileResults {
    return suspendCoroutine { cont ->
        takePicture(
            outputFileOptions,
            MoreExecutors.directExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(outputFileResults)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            })
    }
}

/**
 * Analyze each camera frames.
 *
 * Note that the [Flow] returned here can be collected at most once simultaneously,
 * because [ImageAnalysis] supports only one callback.
 * Caller may use [shareIn] to receive [ImageProxy] at multiple collectors.
 *
 * It's also caller's reponsibility to close delivered [ImageProxy].
 * However, [ImageProxy] which are undelivered will be automatically closed.
 */
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