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
    var camera by remember<MutableState<StableCamera?>> { mutableStateOf(null) }

    // Zoom states
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

    // Torch states
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
        ?: flowOf(null)).collectAsState(null)

    // Side effects for zoom
    LaunchedEffect(zoomState, cameraZoomState) {
        zoomState ?: return@LaunchedEffect
        val camZoomState = cameraZoomState ?: return@LaunchedEffect
        zoomState.ratioRangeFlow.value = camZoomState.run { minZoomRatio..maxZoomRatio }
        if (zoomState.ratioFlow.value == null) {
            zoomState.ratioFlow.compareAndSet(null, CameraAttribute(camZoomState.zoomRatio, true))
        }
    }
    LaunchedEffect(requestZoomRatio, cameraZoomRatio, camera) {
        val newRatio = requestZoomRatio ?: return@LaunchedEffect
        if (cameraZoomRatio != newRatio) {
            camera?.cameraControl?.setZoomRatio(newRatio)
        }
    }

    // Side effects for torch
    LaunchedEffect(torchState, camera) {
        torchState?.hasFlashUnitFlow?.value = camera?.cameraInfo?.hasFlashUnit
    }
    LaunchedEffect(torchState, cameraTorchState) {
        torchState ?: return@LaunchedEffect
        val newOn = cameraTorchState == androidx.camera.core.TorchState.ON
        if (torchState.isOnFlow.value == null) {
            torchState.isOnFlow.compareAndSet(null, CameraAttribute(newOn, true))
        }
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

    var meteringPointFactory by remember { mutableStateOf<MeteringPointFactory?>(null) }

    // Focus metering states
    run {
        focusMeteringState ?: return@run
        val capturedMeteringPointFactory = meteringPointFactory ?: return@run
        val cam = camera ?: return@run

        val requestMeteringParameters by focusMeteringState.meteringParameters.collectAsState()
        val requestMeteringPoints by focusMeteringState.meteringPoints.collectAsState()
        var tapPosition by remember { mutableStateOf<Offset?>(null) }
        val meteringPoints by if (requestMeteringPoints === FocusMeteringState.TapPoint) {
            remember(tapPosition) {
                derivedStateOf {
                    val tap = tapPosition ?: return@derivedStateOf emptyList()
                    listOf(capturedMeteringPointFactory.createPoint(tap.x, tap.y))
                }
            }
        } else {
            remember(requestMeteringPoints) {
                derivedStateOf {
                    requestMeteringPoints.map {
                        capturedMeteringPointFactory.createPoint(it.x, it.y)
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
                    Log.v("asdf", "focus metering...")
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
        onViewCreated = {
            meteringPointFactory = it.meteringPointFactory
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
    onCameraReceived: (StableCamera) -> Unit,
    onViewCreated: (PreviewView) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
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
                        val camera = StableCamera(
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                *newUseCases.toTypedArray()
                            )
                        )
                        onCameraReceived(camera)
                    }
                    view.meteringPointFactory

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

@Stable
private class StableCamera(camera: androidx.camera.core.Camera) : Camera(camera)