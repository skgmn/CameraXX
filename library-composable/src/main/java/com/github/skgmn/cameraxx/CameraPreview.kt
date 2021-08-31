package com.github.skgmn.cameraxx

import android.util.Log
import androidx.camera.core.*
import androidx.camera.core.CameraControl
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
    zoomState: ZoomState? = if (pinchZoomEnabled) remember { ZoomState() } else null,
    torchState: TorchState? = null,
    focusMeteringState: FocusMeteringState? = null
) {
    var m = modifier
    var camera by remember { mutableStateOf<Camera?>(null) }
    var meteringPointFactory by remember { mutableStateOf<MeteringPointFactory?>(null) }

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
        val requestZoomRatio by remember(zoomState) {
            zoomState.ratioFlow
                .filter { it?.fromCamera == false }
                .map { it?.value }
        }.collectAsState(null)
        val ratioRange by remember {
            derivedStateOf { cameraZoomState?.run { minZoomRatio..maxZoomRatio } }
        }

        LaunchedEffect(zoomState, ratioRange) {
            zoomState.ratioRangeFlow.value = ratioRange ?: return@LaunchedEffect
        }
        LaunchedEffect(zoomState, cameraZoomRatio) {
            if (zoomState.ratioFlow.value == null) {
                zoomState.ratioFlow.compareAndSet(
                    null,
                    CameraAttribute(cameraZoomRatio ?: return@LaunchedEffect, true)
                )
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
                                    zoomState.pinchZoomInProgressFlow.value = false
                                }
                            }
                        }
                    }
                    launch {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom == 1f) return@detectTransformGestures
                            val currentRatio =
                                zoomState.ratio.value ?: return@detectTransformGestures
                            val range = ratioRange ?: return@detectTransformGestures

                            zoomState.pinchZoomInProgressFlow.value = true
                            val newRatio = (currentRatio * zoom).coerceIn(range)
                            if (currentRatio != newRatio) {
                                zoomState.ratio.value = newRatio
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
        val requestTorchOn by remember {
            torchState.isOnFlow
                .filter { it?.fromCamera == false }
                .map { it?.value }
        }.collectAsState(null)

        LaunchedEffect(torchState, cam) {
            torchState.hasFlashUnitFlow.value = cam.cameraInfo.hasFlashUnit
        }
        LaunchedEffect(torchState, cameraTorchOn) {
            val on = cameraTorchOn ?: return@LaunchedEffect
            if (torchState.isOnFlow.value == null) {
                torchState.isOnFlow.compareAndSet(null, CameraAttribute(on, true))
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

        val requestMeteringParameters by focusMeteringState.meteringParameters.collectAsState()
        val requestMeteringPoints by focusMeteringState.meteringPoints.collectAsState()
        var tapPosition by remember { mutableStateOf<Offset?>(null) }
        val meteringPoints by if (requestMeteringPoints === FocusMeteringState.TapPoint) {
            remember(tapPosition) {
                derivedStateOf {
                    val tap = tapPosition ?: return@derivedStateOf emptyList()
                    listOf(pointFactory.createPoint(tap.x, tap.y))
                }
            }
        } else {
            remember(requestMeteringPoints) {
                derivedStateOf {
                    requestMeteringPoints.map {
                        pointFactory.createPoint(it.x, it.y)
                    }
                }
            }
        }
        val focusMeteringAction by remember(requestMeteringParameters, meteringPoints) {
            derivedStateOf {
                if (meteringPoints.isEmpty()) return@derivedStateOf null

                val meteringMode = requestMeteringParameters.meteringMode
                val autoCancelDuration = requestMeteringParameters.autoCancelDuration
                var builder = FocusMeteringAction.Builder(meteringPoints[0], meteringMode.value)
                if (meteringPoints.size > 1) {
                    for (i in 1 until meteringPoints.size) {
                        builder = builder.addPoint(meteringPoints[i], meteringMode.value)
                    }
                }
                builder = if (autoCancelDuration == null) {
                    builder.disableAutoCancel()
                } else {
                    builder.setAutoCancelDuration(
                        autoCancelDuration.toLong(DurationUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS
                    )
                }
                builder.build()
            }
        }
        LaunchedEffect(focusMeteringAction) {
            if (focusMeteringState.progressFlow.value == FocusMeteringProgress.InProgress) {
                focusMeteringState.progressFlow.value = FocusMeteringProgress.Cancelled
            }
            cam.cameraControl.cancelFocusAndMetering()
            focusMeteringAction?.let {
                try {
                    focusMeteringState.progressFlow.value = FocusMeteringProgress.InProgress
                    val result = cam.cameraControl.startFocusAndMetering(it)
                    focusMeteringState.progressFlow.value = if (result.isFocusSuccessful) {
                        FocusMeteringProgress.Succeeded
                    } else {
                        FocusMeteringProgress.Failed
                    }
                } catch (e: CameraControl.OperationCanceledException) {
                    focusMeteringState.progressFlow.value = FocusMeteringProgress.Cancelled
                }
            }
        }

        if (requestMeteringPoints === FocusMeteringState.TapPoint) {
            m = m.pointerInput(Unit) {
                detectTapGestures(onTap = { tapPosition = it })
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
        onViewCreated = { meteringPointFactory = it.meteringPointFactory }
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