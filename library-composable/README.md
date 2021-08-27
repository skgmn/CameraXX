# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx-composable:0.4.0"
}
```

# Usage

## CameraPreview function signature

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    preview: Preview?,
    imageCapture: ImageCapture?,
    imageAnalysis: ImageAnalysis?,
    scaleType: PreviewView.ScaleType,
    implementationMode: PreviewView.ImplementationMode,
    pinchZoomEnabled: Boolean,
    zoomState: ZoomState?,
    torchState: TorchState?
)
```

Every parameter has each default values, so just pass only parameters needed.

## Zoom

If you need just simple pinch zoom, just pass true to `pinchZoomEnabled`.

Otherwise refer to this example if you need more customized UI for zoom.

```kotlin
val zoomState by remember { ZoomState() }
val zoomRatioRange by zoomState.ratioRange.collectAsState()
val zoomRatio by zoomState.ratio.collectAsState()

CameraPreview(
    zoomState = zoomState
)
if (zoomRatio != null && zoomRatioRange != null) {
    ZoomControl( // Just an example. Use your own controller UI.
        ratio = zoomRatio,
        ratioRange = zoomRatioRange,
        onZoom = { ratio ->
            zoomState.ratio.value = ratio.coerceIn(zoomRatioRange)
        }
    )
}
```

## Torch

```kotlin
val torchState by remember { TorchState() }
val torchOn by torchState.isOn.collectAsState()
val hasFlashUnit by torchState.hasFlashUnit.collectAsState()

CameraPreview(
    torchState = torchState
)
if (hasFlashUnit == true) {
    Button(
        onClick = { torchState.isOn.value = !torchOn }
    ) {
        Text("Torch " + if (torchOn) "off" else "on")
    }
}
```
