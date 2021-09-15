# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx-bindingadapter:0.6.1"
}
```

# Usage

## Basic usage

```kotlin
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()

    fun onCreate(savedInstanceState: Bundle?) {
        val binding = DataBindingUtil.setContentView(this, R.layout.activity_my)
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
        <variable name="viewModel" type="MyViewModel" />
    </data>

    <androidx.camera.view.PreviewView
        app:cameraSelector="@{viewModel.cameraSelector}"
        app:previewUseCase="@{viewModel.previewUseCase}"
        app:imageCaptureUseCase="@{viewModel.imageCaptureUseCase}"
        app:imageAnalysisUseCase="@{viewModel.imageAnalysisUseCase}" />
    
</layout>
```

## Receive Camera instance

```kotlin
class MyViewModel : ViewModel() {
    private val cameraFlow = MutableStateFlow<Camera?>(null)
    
    fun updateCamera(camera: Camera) {
        cameraFlow.value = camera
    }
}
```
```xml
<androidx.camera.view.PreviewView
    app:onCameraRetrieved="@{camera -> viewModel.updateCamera(camera)}" />
```

Once a `Camera` instance is received, you can get camera informations such as `ZoomState` or `TorchState` through `CameraInfo` or you can control camera by calling methods of `CameraControl`.
You may have to use `mapLatest` or `flatMapLatest` to implement some camera features.
