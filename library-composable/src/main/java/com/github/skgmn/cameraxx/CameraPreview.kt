package com.github.skgmn.cameraxx

import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview? = remember { Preview.Builder().build() },
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE,
    pinchZoomEnabled: Boolean = false,
    cameraReceiver: (suspend (Camera) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val cameraState = remember { mutableStateOf<StableCamera?>(null) }
    var camera by cameraState
    val zoomStateFlow by remember(camera, pinchZoomEnabled) {
        derivedStateOf {
            if (pinchZoomEnabled) {
                camera?.cameraInfo?.getZoomState()
            } else {
                null
            }
        }
    }
    val zoomState by (zoomStateFlow ?: flowOf(null)).collectAsState(null)
    val zoomRatio by remember(zoomState) { derivedStateOf { zoomState?.zoomRatio } }

    LaunchedEffect(camera, cameraReceiver) {
        camera?.let { cameraReceiver?.invoke(it) }
    }

    var m = modifier
    if (pinchZoomEnabled) {
        m = m.pointerInput(Unit) {
            var zoomJob: Job? = null
            detectTransformGestures { _, _, zoom, _ ->
                val cam = camera ?: return@detectTransformGestures
                val currentRatio = zoomRatio ?: return@detectTransformGestures
                val minRatio = zoomState?.minZoomRatio ?: return@detectTransformGestures
                val maxRatio = zoomState?.maxZoomRatio ?: return@detectTransformGestures

                val newRatio = (currentRatio * zoom).coerceIn(minRatio, maxRatio)
                if (currentRatio != newRatio) {
                    zoomJob?.cancel()
                    zoomJob = scope.launch {
                        cam.cameraControl.setZoomRatio(newRatio)
                    }
                }
            }
        }
    }

    AndroidPreviewView(
        m,
        cameraSelector,
        preview,
        imageCapture,
        imageAnalysis,
        scaleType,
        implementationMode
    ) { camera = it }
}

@Composable
private fun AndroidPreviewView(
    modifier: Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview?,
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE,
    onCameraReceived: (StableCamera) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
    val bindings = remember { PreviewViewBindings() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context)
        },
        update = { view ->
            if (view.scaleType != scaleType) {
                view.scaleType = scaleType
            }
            if (view.implementationMode != implementationMode) {
                view.implementationMode = implementationMode
            }

            bindings.bindingJob?.cancel()
            bindings.bindingJob = scope.launch {
                val cameraProvider = view.context.getProcessCameraProvider()
                val oldUseCases: List<UseCase>
                val newUseCases: List<UseCase>
                if (bindings.lifecycleOwner !== lifecycleOwner || bindings.cameraSelector != cameraSelector) {
                    oldUseCases = listOfNotNull(
                        bindings.preview,
                        bindings.imageCapture,
                        bindings.imageAnalysis
                    )
                    newUseCases = listOfNotNull(preview, imageCapture, imageAnalysis)
                } else {
                    oldUseCases = listOfNotNull(
                        bindings.preview?.takeIf { it !== preview },
                        bindings.imageCapture?.takeIf { it !== imageCapture },
                        bindings.imageAnalysis?.takeIf { it !== imageAnalysis }
                    )
                    newUseCases = listOfNotNull(
                        preview.takeIf { it !== bindings.preview },
                        imageCapture?.takeIf { it !== bindings.imageCapture },
                        imageAnalysis?.takeIf { it !== bindings.imageAnalysis }
                    )
                }
                if (oldUseCases.isNotEmpty()) {
                    cameraProvider.unbind(*oldUseCases.toTypedArray())
                }
                if (newUseCases.isNotEmpty()) {
                    val camera = StableCamera(
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            *newUseCases.toTypedArray()
                        )
                    )
                    onCameraReceived(camera)
                }

                if (bindings.preview !== preview) {
                    bindings.preview?.setSurfaceProvider(null)
                    preview?.setSurfaceProvider(view.surfaceProvider)
                }

                bindings.lifecycleOwner = lifecycleOwner
                bindings.cameraSelector = cameraSelector
                bindings.preview = preview
                bindings.imageCapture = imageCapture
                bindings.imageAnalysis = imageAnalysis
            }
        }
    )
}

private class PreviewViewBindings {
    var bindingJob: Job? = null
    var lifecycleOwner: LifecycleOwner? = null
    var cameraSelector: CameraSelector? = null
    var preview: Preview? = null
    var imageCapture: ImageCapture? = null
    var imageAnalysis: ImageAnalysis? = null
}

@Stable
internal class StableCamera(camera: androidx.camera.core.Camera) : Camera(camera)