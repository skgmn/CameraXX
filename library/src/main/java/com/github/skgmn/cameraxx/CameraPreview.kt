package com.github.skgmn.cameraxx

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview = Preview.Builder().build(),
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null
) {
    val composableScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context)
        },
        update = { view ->
            composableScope.launch {
                val oldBindings =
                    view.getTag(R.id.previewViewCameraBindings) as? ComposeCameraBinding
                val oldLifecycleOwner = oldBindings?.lifecycleOwner?.get()
                val oldCameraSelector = oldBindings?.cameraSelector
                val oldPreview = oldBindings?.previewUseCase
                val oldImageCapture = oldBindings?.imageCaptureUseCase
                val oldImageAnalysis = oldBindings?.imageAnalysisUseCase

                if (oldPreview !== preview) {
                    oldPreview?.setSurfaceProvider(null)
                }
                preview.setSurfaceProvider(view.surfaceProvider)

                val cameraProvider = view.context.getProcessCameraProvider()
                if (oldLifecycleOwner !== lifecycleOwner || oldCameraSelector != cameraSelector) {
                    cameraProvider.unbind(oldPreview, oldImageCapture, oldImageAnalysis)
                    val newUseCases = listOfNotNull(preview, imageCapture, imageAnalysis)
                    if (newUseCases.isNotEmpty()) {
                        LocalLifecycleOwner
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            *newUseCases.toTypedArray()
                        )
                    }
                }

                val oldUseCases = listOfNotNull(
                    oldPreview?.takeIf { it !== preview },
                    oldImageCapture?.takeIf { it !== imageCapture },
                    oldImageAnalysis?.takeIf { it !== imageAnalysis }
                )
                cameraProvider.unbind(*oldUseCases.toTypedArray())

                val newUseCases = listOfNotNull(
                    preview.takeIf { it !== oldPreview },
                    imageCapture?.takeIf { it !== oldImageCapture },
                    imageAnalysis?.takeIf { it !== oldImageAnalysis }
                )
                if (newUseCases.isNotEmpty()) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        *newUseCases.toTypedArray()
                    )
                }

                val newBindings = ComposeCameraBinding(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
                view.setTag(R.id.previewViewCameraBindings, newBindings)
            }
        }
    )
}

private class ComposeCameraBinding(
    lifecycleOwner: LifecycleOwner,
    val cameraSelector: CameraSelector,
    val previewUseCase: Preview,
    val imageCaptureUseCase: ImageCapture?,
    val imageAnalysisUseCase: ImageAnalysis?
) {
    val lifecycleOwner: WeakReference<LifecycleOwner> = WeakReference(lifecycleOwner)
}
