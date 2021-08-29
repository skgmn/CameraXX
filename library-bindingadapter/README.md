# Setup

```gradle
dependencies {
    implementation "com.github.skgmn:cameraxx-bindingadapter:0.4.0"
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

## Zoom

```kotlin
class MyViewModel : ViewModel() {
    val zoomRatio = MutableStateFlow<Float?>(null)
    val cameraInfo = MutableStateFlow<CameraInfo?>(null)
    
    val zoomRange = cameraInfo
        .flatMapLatest { it?.getZoomState() ?: emptyFlow() }
        .map { it.minZoomRatio..it.maxZoomRatio }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    fun updateZoom(ratio: Float) {
        zoomRatio.value = ratio.coerceIn(zoomRange ?: return)    
    }
    
    fun zoomBy(scale: Float) {
        zoomRatio.value = ((zoomRatio.value ?: return) * scale).coerceIn(zoomRange ?: return)
    }
}
```

```xml
<layout>

    <data>
        <variable name="viewModel" type="MyViewModel" />
    </data>

    <androidx.camera.view.PreviewView
        app:zoomRatio="@={viewModel.zoomRatio}"
        app:onCameraInfoRetrieved="@{cameraInfo -> viewModel.updateCameraInfo(cameraInfo)}" />
    
</layout>
```

## Torch

```kotlin
class MyViewModel : ViewModel() {
    val torchOn = MutableStateFlow<Float?>(null)
    
    fun toggleTorch() {
        torchOn.value = !(torchOn.value ?: return)
    }
}
```

```xml
<layout>

    <data>
        <variable name="viewModel" type="MyViewModel" />
    </data>

    <androidx.camera.view.PreviewView
        app:torchOn="@={viewModel.torchOn}" />
    
</layout>
```
