package com.github.skgmn.cameraxx

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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
    val requestZoomRatio by (zoomState?.ratioFlow
        ?.filter { it?.fromCamera == false }
        ?.map { it?.value }
        ?: MutableStateFlow(null)).collectAsState(null)

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
    val cameraTorchOn by remember(cameraTorchState) {
        derivedStateOf { cameraTorchState == androidx.camera.core.TorchState.ON }
    }
    val requestTorchOn by (torchState?.isOnFlow
        ?.filter { it?.fromCamera == false }
        ?.map { it?.value }
        ?: MutableStateFlow(null)).collectAsState(null)

    LaunchedEffect(zoomState, cameraZoomState) {
        zoomState ?: return@LaunchedEffect
        zoomState.ratioRangeFlow.value = cameraZoomState?.run { minZoomRatio..maxZoomRatio }
        zoomState.ratioFlow.value = cameraZoomState?.zoomRatio?.let {
            CameraAttribute(it, true)
        }
    }
    LaunchedEffect(requestZoomRatio, cameraZoomRatio, camera) {
        val newRatio = requestZoomRatio ?: return@LaunchedEffect
        if (cameraZoomRatio != newRatio) {
            camera?.cameraControl?.setZoomRatio(newRatio)
        }
    }

    LaunchedEffect(torchState, camera) {
        torchState?.hasFlashUnitFlow?.value = camera?.cameraInfo?.hasFlashUnit
    }
    LaunchedEffect(torchState, cameraTorchState) {
        torchState ?: return@LaunchedEffect
        val newOn = cameraTorchState == androidx.camera.core.TorchState.ON
        torchState.isOnFlow.value = CameraAttribute(newOn, true)
    }
    LaunchedEffect(requestTorchOn, cameraTorchOn, camera) {
        val newOn = requestTorchOn ?: return@LaunchedEffect
        if (cameraTorchOn != newOn) {
            camera?.cameraControl?.enableTorch(newOn)
        }
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
                                zoomState?.pinchZoomInProgressFlow?.value = false
                            }
                        }
                    }
                }
                launch {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom == 1f) return@detectTransformGestures
                        val camRatio = cameraZoomRatio ?: return@detectTransformGestures
                        val minRatio =
                            cameraZoomState?.minZoomRatio ?: return@detectTransformGestures
                        val maxRatio =
                            cameraZoomState?.maxZoomRatio ?: return@detectTransformGestures

                        zoomState?.pinchZoomInProgressFlow?.value = true
                        val newRatio = (camRatio * zoom).coerceIn(minRatio, maxRatio)
                        if (camRatio != newRatio) {
                            zoomState?.ratio?.value = newRatio
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