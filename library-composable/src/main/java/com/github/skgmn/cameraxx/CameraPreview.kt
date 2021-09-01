package com.github.skgmn.cameraxx

import androidx.camera.core.*
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

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
    torchState: TorchState? = null,
    focusMeteringState: FocusMeteringState? = null,
    streamState: StreamState? = null
) {
    val implZoomState = zoomState ?: if (pinchZoomEnabled) remember { ZoomState() } else null
    CameraPreviewImpl(
        modifier,
        cameraSelector,
        preview,
        imageCapture,
        imageAnalysis,
        scaleType,
        implementationMode,
        pinchZoomEnabled,
        implZoomState,
        torchState,
        focusMeteringState,
        streamState
    )
}

@Composable
private fun CameraPreviewImpl(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    preview: Preview?,
    imageCapture: ImageCapture?,
    imageAnalysis: ImageAnalysis?,
    scaleType: PreviewView.ScaleType,
    implementationMode: PreviewView.ImplementationMode,
    pinchZoomEnabled: Boolean,
    zoomState: ZoomState?,
    torchState: TorchState?,
    focusMeteringState: FocusMeteringState?,
    streamState: StreamState?
) {
    var m = modifier
    var camera by remember { mutableStateOf<Camera?>(null) }
    var meteringPointFactory by remember { mutableStateOf<MeteringPointFactory?>(null) }
    var previewStreamStateFlow by remember { mutableStateOf<Flow<PreviewView.StreamState>?>(null) }

    run zoom@{
        zoomState ?: return@zoom
        val cam = camera ?: return@zoom

        val cameraZoomState by remember(cam) {
            cam.cameraInfo.getZoomState()
                .distinctUntilChanged { old, new ->
                    old.minZoomRatio == new.minZoomRatio &&
                            old.maxZoomRatio == new.maxZoomRatio &&
                            old.zoomRatio == new.zoomRatio
                }
        }.collectAsState(null)
        val cameraZoomRatio by remember { derivedStateOf { cameraZoomState?.zoomRatio } }
        val cameraRatioRange by remember {
            derivedStateOf { cameraZoomState?.run { minZoomRatio..maxZoomRatio } }
        }
        val requestZoomRatio by remember { derivedStateOf { zoomState.ratio } }

        LaunchedEffect(zoomState, cameraRatioRange) {
            zoomState.ratioRangeState.value = cameraRatioRange ?: return@LaunchedEffect
        }
        if (requestZoomRatio == null) {
            LaunchedEffect(zoomState, cameraZoomRatio) {
                if (zoomState.ratio == null && cameraZoomRatio != null) {
                    zoomState.ratio = cameraZoomRatio
                }
            }
        }
        LaunchedEffect(requestZoomRatio, cameraZoomRatio, cam) {
            val newRatio = requestZoomRatio ?: return@LaunchedEffect
            if (cameraZoomRatio != newRatio) {
                cam.cameraControl.setZoomRatio(newRatio)
            }
        }

        if (pinchZoomEnabled) {
            m = m.pointerInput(Unit) {
                coroutineScope {
                    launch {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.any { it.pressed }
                                if (!pressed) {
                                    zoomState.pinchZoomInProgressState.value = false
                                }
                            }
                        }
                    }
                    launch {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom == 1f) return@detectTransformGestures
                            val currentRatio = requestZoomRatio ?: return@detectTransformGestures
                            val range = cameraRatioRange ?: return@detectTransformGestures

                            zoomState.pinchZoomInProgressState.value = true
                            val newRatio = (currentRatio * zoom).coerceIn(range)
                            if (currentRatio != newRatio) {
                                zoomState.ratio = newRatio
                            }
                        }
                    }
                }
            }
        }
    }

    run torch@{
        torchState ?: return@torch
        val cam = camera ?: return@torch

        val cameraTorchOn by remember {
            cam.cameraInfo.getTorchState()
                .map { it == androidx.camera.core.TorchState.ON }
        }.collectAsState(null)
        val requestTorchOn by remember { derivedStateOf { torchState.isOn } }

        LaunchedEffect(torchState, cam) {
            torchState.hasFlashUnitState.value = cam.cameraInfo.hasFlashUnit
        }
        if (requestTorchOn == null) {
            LaunchedEffect(torchState, cameraTorchOn) {
                val on = cameraTorchOn ?: return@LaunchedEffect
                if (torchState.isOnState.value == null) {
                    torchState.isOnState.value = on
                }
            }
        }
        LaunchedEffect(requestTorchOn, cameraTorchOn, cam) {
            val on = requestTorchOn ?: return@LaunchedEffect
            if (cameraTorchOn != on) {
                cam.cameraControl.enableTorch(on)
            }
        }
    }

    run focusMetering@{
        focusMeteringState ?: return@focusMetering
        val pointFactory = meteringPointFactory ?: return@focusMetering
        val cam = camera ?: return@focusMetering

        val requestMeteringParameters = focusMeteringState.parameters
        val requestMeteringPoints = focusMeteringState.meteringPoints
        val meteringPoints by remember {
            derivedStateOf {
                requestMeteringPoints.getOffsetListState().value.map {
                    pointFactory.createPoint(it.x, it.y)
                }
            }
        }
        val focusMeteringAction by remember {
            derivedStateOf {
                if (meteringPoints.isEmpty()) return@derivedStateOf null

                val meteringMode = requestMeteringParameters.meteringMode
                if (meteringMode == MeteringMode.None) return@derivedStateOf null

                val autoCancelDurationMs = requestMeteringParameters.autoCancelDurationMs
                var builder = FocusMeteringAction.Builder(meteringPoints[0], meteringMode.value)
                if (meteringPoints.size > 1) {
                    for (i in 1 until meteringPoints.size) {
                        builder = builder.addPoint(meteringPoints[i], meteringMode.value)
                    }
                }
                builder = if (autoCancelDurationMs == null) {
                    builder.disableAutoCancel()
                } else {
                    builder.setAutoCancelDuration(autoCancelDurationMs, TimeUnit.MILLISECONDS)
                }
                builder.build()
            }
        }
        LaunchedEffect(focusMeteringAction, cam) {
            if (focusMeteringState.progressState.value == FocusMeteringProgress.InProgress) {
                focusMeteringState.progressState.value = FocusMeteringProgress.Cancelled
            }
            try {
                cam.cameraControl.cancelFocusAndMetering()
            } catch (e: OperationCanceledException) {
                // Camera is not active. Ignore it.
            }
            focusMeteringAction?.let {
                try {
                    focusMeteringState.progressState.value = FocusMeteringProgress.InProgress
                    val result = cam.cameraControl.startFocusAndMetering(it)
                    focusMeteringState.progressState.value = if (result.isFocusSuccessful) {
                        FocusMeteringProgress.Succeeded
                    } else {
                        FocusMeteringProgress.Failed
                    }
                } catch (e: OperationCanceledException) {
                    focusMeteringState.progressState.value = FocusMeteringProgress.Cancelled
                } catch (e: Throwable) {
                    focusMeteringState.progressState.value = FocusMeteringProgress.Failed
                }
            }
        }

        (requestMeteringPoints as? TapMeteringPoints)?.let { points ->
            m = m.pointerInput(Unit) {
                detectTapGestures(onTap = {
                    points.tapOffsetState.value = it
                })
            }
        }
    }

    run previewStreamState@{
        streamState ?: return@previewStreamState
        LaunchedEffect(previewStreamStateFlow) {
            previewStreamStateFlow?.collect {
                streamState.isStreamingState.value = it == PreviewView.StreamState.STREAMING
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
        implementationMode,
        onCameraReceived = { camera = it },
        onViewCreated = {
            meteringPointFactory = it.meteringPointFactory
            previewStreamStateFlow = it.listenPreviewStreamState()
        }
    )
}

@Composable
private fun AndroidPreviewView(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    preview: Preview?,
    imageCapture: ImageCapture?,
    imageAnalysis: ImageAnalysis?,
    scaleType: PreviewView.ScaleType,
    implementationMode: PreviewView.ImplementationMode,
    onCameraReceived: (Camera) -> Unit,
    onViewCreated: (PreviewView) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val bindings = remember { PreviewViewBindings() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).also {
                onViewCreated(it)
            }
        },
        update = { view ->
            if (view.scaleType != scaleType) {
                view.scaleType = scaleType
            }
            if (view.implementationMode != implementationMode) {
                view.implementationMode = implementationMode
            }

            val oldUseCases: List<UseCase>
            val newUseCases: List<UseCase>
            if (bindings.lifecycleOwner !== lifecycleOwner ||
                bindings.cameraSelector != cameraSelector
            ) {
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

            if (oldUseCases.isNotEmpty() || newUseCases.isNotEmpty()) {
                bindings.bindingJob?.cancel()
                bindings.bindingJob = scope.launch(Dispatchers.Main.immediate) {
                    val cameraProvider = view.context.getProcessCameraProvider()
                    if (oldUseCases.isNotEmpty()) {
                        cameraProvider.unbind(*oldUseCases.toTypedArray())
                    }
                    if (newUseCases.isNotEmpty()) {
                        val camera = Camera(
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