# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx:0.6.0"
}
```

# Features

CameraXX provides extensions methods for CameraX to use functions with coroutines and flows.

```kotlin
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider
fun PreviewView.listenPreviewStreamState(): Flow<PreviewView.StreamState>
suspend fun ImageCapture.takePicture(): ImageProxy
suspend fun ImageCapture.takePicture(ImageCapture.OutputFileOptions): ImageCapture.OutputFileResults
fun ImageAnalysis.analyze(): Flow<ImageProxy>
```

* [Using with Jetpack Compose](https://github.com/skgmn/CameraXX/tree/master/library-composable)
* [Using with DataBinding](https://github.com/skgmn/CameraXX/tree/master/library-bindingadapter)
