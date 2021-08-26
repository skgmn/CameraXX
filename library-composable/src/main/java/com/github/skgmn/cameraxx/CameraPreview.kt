package com.github.skgmn.cameraxx

import android.util.Log
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    zoomState: ZoomState? = null,
    torchState: TorchState? = null
) {
    val cameraState = remember { mutableStateOf<StableCamera?>(null) }
    var camera by cameraState

    val cameraZoomStateFlow by remember(camera, pinchZoomEnabled, zoomState) {
        derivedStateOf {
            if (pinchZoomEnabled || zoomState != null) {
                camera?.cameraInfo?.getZoomState()
            } else {
                null
            }
        }
    }
    val cameraZoomState by (cameraZoomStateFlow ?: flowOf(null)).collectAsState(null)
    val cameraZoomRatio by remember(cameraZoomState) {
        derivedStateOf { cameraZoomState?.zoomRatio }
    }

    val cameraTorchStateFlow by remember(camera, torchState) {
        derivedStateOf {
            if (torchState != null) {
                camera?.cameraInfo?.getTorchState()
            } else {
                null
            }
        }
    }
    val cameraTorchState by (cameraTorchStateFlow ?: flowOf(null)).collectAsState(null)
    val requestTorchOn by (torchState?.isOn ?: MutableStateFlow(null)).collectAsState()

    LaunchedEffect(zoomState, camera) {
        zoomState?.cameraFlow?.value = camera
    }
    LaunchedEffect(zoomState, cameraZoomState) {
        zoomState?._ratioRange?.value = cameraZoomState?.run { minZoomRatio..maxZoomRatio }
        zoomState?._ratio?.value = cameraZoomState?.zoomRatio
    }
    LaunchedEffect(torchState, camera) {
        torchState?._hasFlashUnit?.value = camera?.cameraInfo?.hasFlashUnit
    }

    LaunchedEffect(torchState, cameraTorchState) {
        torchState?.isOn?.value = cameraTorchState == androidx.camera.core.TorchState.ON
    }
    LaunchedEffect(requestTorchOn, camera) {
        requestTorchOn?.let { camera?.cameraControl?.enableTorch(it) }
    }

    var m = modifier
    if (pinchZoomEnabled) {
        m = m.pointerInput(Unit) {
            coroutineScope {
                launch {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.any { it.pressed }
                            if (!pressed) {
                                zoomState?._pinchZoomInProgress?.value = false
                            }
                        }
                    }
                }
                launch {
                    var zoomJob: Job? = null
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom == 1f) return@detectTransformGestures
                        val cam = camera ?: return@detectTransformGestures
                        val currentRatio = cameraZoomRatio ?: return@detectTransformGestures
                        val minRatio =
                            cameraZoomState?.minZoomRatio ?: return@detectTransformGestures
                        val maxRatio =
                            cameraZoomState?.maxZoomRatio ?: return@detectTransformGestures

                        zoomState?._pinchZoomInProgress?.value = true
                        val newRatio = (currentRatio * zoom).coerceIn(minRatio, maxRatio)
                        if (currentRatio != newRatio) {
                            zoomJob?.cancel()
                            zoomJob = launch {
                                cam.cameraControl.setZoomRatio(newRatio)
                            }
                        }
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