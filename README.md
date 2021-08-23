# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx:0.2.0"
    implementation "com.github.skgmn:cameraxx-bindingadapter:0.2.0" // for Databinding
    implementation "com.github.skgmn:cameraxx-composable:0.2.0"     // for Jetpack Compose
}
```

# Features

### Extension methods providing coroutine and flow

```kotlin
suspend fun Context.getProcessCameraProvider(): ProcessCameraProvider
suspend fun ImageCapture.takePicture(): ImageProxy
suspend fun ImageCapture.takePicture(ImageCapture.OutputFileOptions): ImageCapture.OutputFileResults
fun ImageAnalysis.analyze(): Flow<ImageProxy>
```

### Composable

```kotlin
CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    preview: Preview?,
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null
)
```

If you omit `preview`, it creates a default `Preview` instance and uses it.

```kotlin
CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null
)
```

### BindingAdapter

```kotlin
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()

    fun onCreate(savedInstanceState: Bundle?) {
        val binding = DataBindingUtil.setContentView(this, R.layout.activity_my)
        binding.owner = this
        binding.viewModel = viewModel
    }
}

class MyViewModel : ViewModel() {
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    val previewUseCase = Preview.Builder().build()
    val imageCaptureUseCase = ImageCapture.Builder().build()
    val imageAnalysisUseCase = ImageAnalysis.Builder().build()
}
```

```xml
<layout>

    <data>
        <variable name="owner" type="androidx.lifecycle.LifecycleOwner" />
        <variable name="viewModel" type="MyViewModel" />
    </data>

    <androidx.camera.view.PreviewView
        app:lifecycleOwner="@{owner}"
        app:cameraSelector="@{viewModel.cameraSelector}"
        app:previewUseCase="@{viewModel.previewUseCase}"
        app:imageCaptureUseCase="@{viewModel.imageCaptureUseCase}"
        app:imageAnalysisUseCase="@{viewModel.imageAnalysisUseCase}" />
    
</layout>
```
