package com.github.skgmn.cameraxx

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import com.github.skgmn.cameraxx.bindingadapter.R
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

object PreviewViewBindingAdapters {
    @JvmStatic
    @DelicateCoroutinesApi
    @BindingAdapter(
        "lifecycleOwner",
        "cameraSelector",
        "previewUseCase",
        "imageCaptureUseCase",
        "imageAnalysisUseCase",
        requireAll = false
    )
    fun PreviewView.bind(
        newLifecycleOwner: LifecycleOwner?,
        newCameraSelector: CameraSelector?,
        newPreview: Preview?,
        newImageCapture: ImageCapture?,
        newImageAnalysis: ImageAnalysis?
    ) {
        newLifecycleOwner ?: throw IllegalArgumentException("lifecycleOwner missing")
        newCameraSelector ?: throw IllegalArgumentException("cameraSelector missing")

        val prevJob = getTag(R.id.previewViewPrevBindingJob) as? Job
        prevJob?.cancel()

        val oldBindings = getTag(R.id.previewViewCameraBindings) as? CameraBindings

        val newJob = GlobalScope.launch(Dispatchers.Main.immediate, start = CoroutineStart.LAZY) {
            val oldLifecycleOwner = oldBindings?.lifecycleOwner?.get()
            val oldCameraSelector = oldBindings?.cameraSelector
            val oldPreview = oldBindings?.previewUseCase
            val oldImageCapture = oldBindings?.imageCaptureUseCase
            val oldImageAnalysis = oldBindings?.imageAnalysisUseCase

            if (oldPreview !== newPreview) {
                oldPreview?.setSurfaceProvider(null)
            }
            newPreview?.setSurfaceProvider(surfaceProvider)

            val cameraProvider = context.getProcessCameraProvider()

            if (oldLifecycleOwner !== newLifecycleOwner || oldCameraSelector != newCameraSelector) {
                cameraProvider.unbind(oldPreview, oldImageCapture, oldImageAnalysis)
                val newUseCases = listOfNotNull(newPreview, newImageCapture, newImageAnalysis)
                if (newUseCases.isNotEmpty()) {
                    cameraProvider.bindToLifecycle(
                        newLifecycleOwner,
                        newCameraSelector,
                        *newUseCases.toTypedArray()
                    )
                }
            }

            val oldUseCases = listOfNotNull(
                oldPreview?.takeIf { it !== newPreview },
                oldImageCapture?.takeIf { it !== newImageCapture },
                oldImageAnalysis?.takeIf { it !== newImageAnalysis }
            )
            cameraProvider.unbind(*oldUseCases.toTypedArray())

            val newUseCases = listOfNotNull(
                newPreview?.takeIf { it !== oldPreview },
                newImageCapture?.takeIf { it !== oldImageCapture },
                newImageAnalysis?.takeIf { it !== oldImageAnalysis }
            )
            if (newUseCases.isNotEmpty()) {
                cameraProvider.bindToLifecycle(
                    newLifecycleOwner,
                    newCameraSelector,
                    *newUseCases.toTypedArray()
                )
            }

            val newBindings = CameraBindings(
                newLifecycleOwner,
                newCameraSelector,
                newPreview,
                newImageCapture,
                newImageAnalysis
            )
            setTag(R.id.previewViewCameraBindings, newBindings)
        }
        setTag(R.id.previewViewPrevBindingJob, newJob)
        newJob.start()
    }

    private class CameraBindings(
        lifecycleOwner: LifecycleOwner?,
        val cameraSelector: CameraSelector?,
        val previewUseCase: Preview?,
        val imageCaptureUseCase: ImageCapture?,
        val imageAnalysisUseCase: ImageAnalysis?
    ) {
        val lifecycleOwner: WeakReference<LifecycleOwner>? =
            lifecycleOwner?.let { WeakReference(it) }
    }
}