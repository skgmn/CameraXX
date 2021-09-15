# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx-composable:0.6.0"
}
```

# Usage

## CameraPreview function signature

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview? = remember { Preview.Builder().build() },
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE,
    zoomState: ZoomState? = null,
    torchState: TorchState? = null,
    focusMeteringState: FocusMeteringState? = null,
    previewStreamState: PreviewStreamState? = null
)
```

Every parameter has each default values, so just pass only parameters needed.

## Zoom

### Pinch to zoom

```kotlin
val zoomState by remember { ZoomState(pinchZoomEnabled = true) }

CameraPreview(zoomState = zoomState)
```

You can enable/disable it by setting `zoomState.pinchZoomEnabled` at runtime.

### Custom UI

```kotlin
val zoomState by remember { ZoomState() }

CameraPreview(zoomState = zoomState)
run {
    Slider(
        value = zoomState.ratio ?: return@run,
        valueRange = zoomState.ratioRange ?: return@run,
        onValueChange = { zoomState.ratio = it }
    )
}
```

## Torch

```kotlin
val torchState by remember { TorchState() }

CameraPreview(torchState = torchState)
if (hasFlashUnit == true) {
    run {
        val on = torchState.isOn ?: return@run
        Button(
            onClick = { torchState.isOn = !on }
        ) {
            Text("Torch " + if (on) "off" else "on")
        }
    }
}
```

## Focus metering

### Tap to focus

```kotlin
val focusMeteringState by remember { FocusMeteringState() }

CameraPreview(focusMeteringState = focusMeteringState)
```

Providing `FocusMeteringState` with all default parameters will allow user to tap screen to focus on that area.
If it is needed to show an indicator at tap location, use explicit `TapMeteringPoints` and get value from `TapMeteringPoints.tapOffset`

```kotlin
val meteringPoints by remember { TapMeteringPoints() }
val focusMeteringState by remember { FocusMeteringState(meteringPoints = meteringPoints) }

CameraPreview(focusMeteringState = focusMeteringState)
// Show UI on meteringPoints.tapOffset
```

### Manually pass coordinates

Use `ManualMeteringPoints`

```kotlin
val meteringPoints by remember { ManualMeteringPoints() }
val focusMeteringState by remember { FocusMeteringState(meteringPoints = meteringPoints) }

CameraPreview(focusMeteringState = focusMeteringState)
// offer coordinates as List<Offset> to meteringPoints.offsets
```
