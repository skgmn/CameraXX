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
fun <T> ImageAnalysis.analyze(suspend (ImageProxy) -> T): Flow<T>
fun ImageProxy.toBitmap(): Bitmap
fun ImageProxy.toByteArray(): ByteArray
fun PreviewView.listenPreviewStreamState(): StateFlow<PreviewView.StreamState?>
```

* [Using with Jetpack Compose](https://github.com/skgmn/CameraXX/tree/master/library-composable)
* [Using with DataBinding](https://github.com/skgmn/CameraXX/tree/master/library-bindingadapter)
