package com.github.skgmn.cameraxx

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import java.lang.ref.WeakReference
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@GuardedBy("analyzerFlows")
private val analyzerFlows = WeakHashMap<ImageAnalysis, WeakReference<Flow<ImageProxy>>>()

suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider {
    return ProcessCameraProvider.getInstance(this).await(false)
}

fun PreviewView.listenPreviewStreamState(): Flow<PreviewView.StreamState> {
    return previewStreamState.toFlow()
}

suspend fun ImageCapture.takePicture(): ImageProxy {
    return suspendCoroutine { cont ->
        takePicture(ImmediateExecutor(), object : ImageCapture.OnImageCapturedCallback() {
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
            ImmediateExecutor(),
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
            setAnalyzer(ImmediateExecutor(), {
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