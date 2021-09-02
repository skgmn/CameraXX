# Setup

GitHub Packages authorization is required to use this library. See [this](https://gist.github.com/skgmn/79da4a935e904078491e932bd5b327c7) to setup.

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx:0.5.0"
}
```

# Features

### Extension methods providing coroutine and flow

```kotlin
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider
fun PreviewView.listenPreviewStreamState(): Flow<PreviewView.StreamState>
suspend fun ImageCapture.takePicture(): ImageProxy
suspend fun ImageCapture.takePicture(ImageCapture.OutputFileOptions): ImageCapture.OutputFileResults
fun ImageAnalysis.analyze(): Flow<ImageProxy>
```

### Composable

See [this](https://github.com/skgmn/CameraXX/tree/develop/library-composable).

### BindingAdapter

See [this](https://github.com/skgmn/CameraXX/tree/develop/library-bindingadapter).
