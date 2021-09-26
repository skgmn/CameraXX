# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx:0.7.0"
}
```

# Features

CameraXX provides extensions methods for CameraX to use functions with coroutines and flows.

```kotlin
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider
suspend fun ImageCapture.takePicture(): ImageProxy
suspend fun ImageCapture.takePicture(ImageCapture.OutputFileOptions): ImageCapture.OutputFileResults
fun ImageAnalysis.analyze(): Flow<ImageProxy>
fun ImageProxy.toBitmap(): Bitmap
fun ImageProxy.toByteArray(): ByteArray
fun PreviewView.listenPreviewStreamState(): StateFlow<PreviewView.StreamState?>
```

* [Using with Jetpack Compose](https://github.com/skgmn/CameraXX/tree/develop/library-composable)
* [Using with DataBinding](https://github.com/skgmn/CameraXX/tree/develop/library-bindingadapter)