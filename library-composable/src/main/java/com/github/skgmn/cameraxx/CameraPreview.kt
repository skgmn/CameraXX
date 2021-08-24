package com.github.skgmn.cameraxx

import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE
) {
    val defaultPreview by remember { mutableStateOf(Preview.Builder().build()) }
    CameraPreview(
        modifier,
        cameraSelector,
        defaultPreview,
        imageCapture,
        imageAnalysis,
        scaleType,
        implementationMode
    )
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview?,
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE
) {
    val composableScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var updateJob by remember { mutableStateOf<Job?>(null) }
    var currentLifecycleOwner by remember { mutableStateOf<LifecycleOwner?>(null) }
    var currentCameraSelector by remember { mutableStateOf<CameraSelector?>(null) }
    var currentPreview by remember { mutableStateOf<Preview?>(null) }
    var currentImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var currentImageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var currentScaleType by remember { mutableStateOf<PreviewView.ScaleType?>(null) }
    var currentImplMode by remember { mutableStateOf<PreviewView.ImplementationMode?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context)
        },
        update = { view ->
            if (currentScaleType != scaleType) {
                view.scaleType = scaleType
                currentScaleType = scaleType
            }
            if (currentImplMode != implementationMode) {
                view.implementationMode = implementationMode
                currentImplMode = implementationMode
            }

            updateJob?.cancel()
            updateJob = composableScope.launch {
                val cameraProvider = view.context.getProcessCameraProvider()
                val oldUseCases: List<UseCase>
                val newUseCases: List<UseCase>
                if (currentLifecycleOwner !== lifecycleOwner || currentCameraSelector != cameraSelector) {
                    oldUseCases = listOfNotNull(currentPreview, currentImageCapture, currentImageAnalysis)
                    newUseCases = listOfNotNull(preview, imageCapture, imageAnalysis)
                } else {
                    oldUseCases = listOfNotNull(
                        currentPreview?.takeIf { it !== preview },
                        currentImageCapture?.takeIf { it !== imageCapture },
                        currentImageAnalysis?.takeIf { it !== imageAnalysis }
                    )
                    newUseCases = listOfNotNull(
                        preview.takeIf { it !== currentPreview },
                        imageCapture?.takeIf { it !== currentImageCapture },
                        imageAnalysis?.takeIf { it !== currentImageAnalysis }
                    )
                }
                if (oldUseCases.isNotEmpty()) {
                    cameraProvider.unbind(*oldUseCases.toTypedArray())
                }
                if (newUseCases.isNotEmpty()) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        *newUseCases.toTypedArray()
                    )
                }

                if (currentPreview !== preview) {
                    currentPreview?.setSurfaceProvider(null)
                    preview?.setSurfaceProvider(view.surfaceProvider)
                }

                currentLifecycleOwner = lifecycleOwner
                currentCameraSelector = cameraSelector
                currentPreview = preview
                currentImageCapture = imageCapture
                currentImageAnalysis = imageAnalysis
            }
        }
    )
}