# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx:0.2.0"
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

### BindingAdapters

```kotlin
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()

    fun onCreate(savedInstanceState: Bundle?) {
        val binding = DataBindingUtil.setContentView(this, R.layout.activity_my)
        binding.lifecycleOwnerForBinding = this
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
        <variable name="lifecycleOwnerForBinding" type="androidx.lifecycle.LifecycleOwner" />
        <variable name="viewModel" type="MyViewModel" />
    </data>

    <androidx.camera.view.PreviewView
        app:lifecycleOwner="@{lifecycleOwnerForBinding}"
        app:cameraSelector="@{viewModel.cameraSelector}"
        app:previewUseCase="@{viewModel.previewUseCase}"
        app:imageCaptureUseCase="@{viewModel.imageCaptureUseCase}"
        app:imageAnalysisUseCase="@{viewModel.imageAnalysisUseCase}" />
    
</layout>
```
