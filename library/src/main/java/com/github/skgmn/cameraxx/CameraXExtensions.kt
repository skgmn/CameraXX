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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("UnstableApiUsage")
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(this)
    return Futures.nonCancellationPropagating(future).await()
}

fun PreviewView.listenPreviewStreamState(): Flow<PreviewView.StreamState> {
    return previewStreamState.toFlow()
}

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

@OptIn(ExperimentalCoroutinesApi::class)
fun ImageAnalysis.analyze(): Flow<ImageProxy> {
    return callbackFlow {
        val imageProxies = Collections.newSetFromMap(WeakHashMap<ImageProxy, Boolean>())
        setAnalyzer(MoreExecutors.directExecutor(), { imageProxy ->
            val imageProxyWrapper = ImageProxyWrapper.wrap(imageProxy)
            imageProxies += imageProxyWrapper
            ImageProxyWrapper.addOnCloseListener(imageProxyWrapper) {
                imageProxies -= imageProxyWrapper
            }
            if (!trySend(imageProxyWrapper).isSuccess) {
                imageProxies -= imageProxyWrapper
                imageProxyWrapper.close()
            }
        })
        awaitClose {
            imageProxies.forEach { it.close() }
            imageProxies.clear()
            clearAnalyzer()
        }
    }
}