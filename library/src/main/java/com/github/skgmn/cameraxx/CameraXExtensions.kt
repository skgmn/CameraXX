package com.github.skgmn.cameraxx

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.guava.await
import java.lang.ref.WeakReference
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@GuardedBy("analyzerFlows")
private val analyzerFlows = WeakHashMap<ImageAnalysis, WeakReference<Flow<ImageProxy>>>()

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
        takePicture(MoreExecutors.directExecutor(), object : ImageCapture.OnImageCapturedCallback() {
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

@OptIn(
    ExperimentalCoroutinesApi::class,
    DelicateCoroutinesApi::class
)
fun ImageAnalysis.analyze(): Flow<ImageProxy> {
    return synchronized(analyzerFlows) {
        analyzerFlows[this]?.get() ?: callbackFlow {
            setAnalyzer(MoreExecutors.directExecutor(), {
                trySend(it)
            })
            awaitClose {
                clearAnalyzer()
            }
        }.shareIn(GlobalScope, SharingStarted.WhileSubscribed(), 0).also {
            analyzerFlows[this] = WeakReference(it)
        }
    }
}