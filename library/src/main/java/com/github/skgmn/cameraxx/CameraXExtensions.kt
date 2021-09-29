package com.github.skgmn.cameraxx

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.github.skgmn.coroutineskit.lifecycle.toStateFlow
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
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