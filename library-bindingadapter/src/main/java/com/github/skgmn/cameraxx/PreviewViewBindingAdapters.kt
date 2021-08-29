package com.github.skgmn.cameraxx

import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.github.skgmn.cameraxx.bindingadapter.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
object PreviewViewBindingAdapters {
    @JvmStatic
    @BindingAdapter(
        "cameraSelector",
        "previewUseCase",
        "imageCaptureUseCase",
        "imageAnalysisUseCase",
        requireAll = false
    )
    fun PreviewView.bindUseCases(
        newCameraSelector: CameraSelector?,
        newPreview: Preview?,
        newImageCapture: ImageCapture?,
        newImageAnalysis: ImageAnalysis?
    ) {
        requireNotNull(newCameraSelector) { "cameraSelector missing" }
        val newLifecycleOwner = findLifecycleOwner()

        val bindings = getTag(R.id.previewViewBindings) as? Bindings
        val oldLifecycleOwner = bindings?.lifecycleOwner?.get()
        val oldCameraSelector = bindings?.cameraSelector
        val oldPreview = bindings?.preview
        val oldImageCapture = bindings?.imageCapture
        val oldImageAnalysis = bindings?.imageAnalysis

        val oldUseCases: List<UseCase>
        val newUseCases: List<UseCase>
        if (oldLifecycleOwner !== newLifecycleOwner || oldCameraSelector != newCameraSelector) {
            oldUseCases = listOfNotNull(oldPreview, oldImageCapture, oldImageAnalysis)
            newUseCases = listOfNotNull(newPreview, newImageCapture, newImageAnalysis)
        } else {
            oldUseCases = listOfNotNull(
                oldPreview?.takeIf { it !== newPreview },
                oldImageCapture?.takeIf { it !== newImageCapture },
                oldImageAnalysis?.takeIf { it !== newImageAnalysis }
            )
            newUseCases = listOfNotNull(
                newPreview.takeIf { it !== oldPreview },
                newImageCapture?.takeIf { it !== oldImageCapture },
                newImageAnalysis?.takeIf { it !== oldImageAnalysis }
            )
        }

        if (oldUseCases.isNotEmpty() || newUseCases.isNotEmpty()) {
            (getTag(R.id.previewViewBindingJob) as? Job)?.cancel()
            val newJob = newLifecycleOwner.lifecycleScope.launch(
                context = Dispatchers.Main.immediate,
                start = CoroutineStart.LAZY
            ) {
                val cameraProvider = context.getProcessCameraProvider()
                if (oldUseCases.isNotEmpty()) {
                    cameraProvider.unbind(*oldUseCases.toTypedArray())
                }
                if (newUseCases.isNotEmpty()) {
                    val camera = Camera(
                        cameraProvider.bindToLifecycle(
                            newLifecycleOwner,
                            newCameraSelector,
                            *newUseCases.toTypedArray()
                        )
                    )
                    @Suppress("UNCHECKED_CAST")
                    (getTag(R.id.previewViewCameraFlow) as? MutableStateFlow<Camera?>)?.let {
                        it.value = camera
                    } ?: MutableStateFlow(camera).also {
                        setTag(R.id.previewViewCameraFlow, it)
                    }
                }

                if (oldPreview !== newPreview) {
                    oldPreview?.setSurfaceProvider(null)
                    newPreview?.setSurfaceProvider(surfaceProvider)
                }

                val newBindings = Bindings(
                    newLifecycleOwner,
                    newCameraSelector,
                    newPreview,
                    newImageCapture,
                    newImageAnalysis
                )
                setTag(R.id.previewViewBindings, newBindings)
            }
            setTag(R.id.previewViewBindingJob, newJob)
            newJob.start()
        }
    }

    @JvmStatic
    @BindingAdapter(
        "zoomRatio",
        "zoomRatioAttrChanged",
        requireAll = false
    )
    fun PreviewView.bindZoomRatio(
        zoomRatio: Float?,
        zoomRatioAttrChanged: InverseBindingListener?,
    ) {
        val lifecycleOwner = findLifecycleOwner()

        (getTag(R.id.previewViewZoomJob) as? Job)?.cancel()
        val job = lifecycleOwner.lifecycleScope.launch(start = CoroutineStart.LAZY) {
            lifecycleOwner.whenStarted {
                val cameraZoomRatio = MutableStateFlow(
                    getTag(R.id.previewViewZoomRatio) as? Float
                )
                launch {
                    cameraFlow
                        .flatMapLatest { it?.cameraInfo?.getZoomState() ?: emptyFlow() }
                        .collect {
                            cameraZoomRatio.value = it.zoomRatio
                            setTag(R.id.previewViewZoomRatio, it.zoomRatio)
                            if (zoomRatio == null) {
                                zoomRatioAttrChanged?.onChange()
                            }
                        }
                }
                if (zoomRatio != null) {
                    launch {
                        combine(
                            cameraFlow.filterNotNull(),
                            cameraZoomRatio
                        ) { camera, camZoomRatio ->
                            if (camZoomRatio != zoomRatio) {
                                suspend { camera.cameraControl.setZoomRatio(zoomRatio) }
                            } else {
                                null
                            }
                        }.collect {
                            it?.invoke()
                        }
                    }
                }
            }
        }
        setTag(R.id.previewViewZoomJob, job)
        job.start()
    }

    @JvmStatic
    @BindingAdapter("onZoomRangeChanged")
    fun PreviewView.bindListeners(
        onZoomRangeChanged: OnZoomRangeChangedListener
    ) {
        val lifecycleOwner = findLifecycleOwner()

        (getTag(R.id.previewViewListenerJob) as? Job)?.cancel()
        lifecycleOwner.lifecycleScope.launch {
            cameraFlow
                .flatMapLatest { it?.cameraInfo?.getZoomState() ?: emptyFlow() }
                .collect { onZoomRangeChanged.onZoomRangeChanged(it.minZoomRatio..it.maxZoomRatio) }
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "zoomRatio")
    fun PreviewView.getFirstZoomRatio(): Float? {
        return getTag(R.id.previewViewZoomRatio) as? Float
    }

    private val PreviewView.cameraFlow: Flow<Camera?>
        get() {
            @Suppress("UNCHECKED_CAST")
            return getTag(R.id.previewViewCameraFlow) as? MutableStateFlow<Camera?>
                ?: MutableStateFlow<Camera?>(null).also {
                    setTag(R.id.previewViewCameraFlow, it)
                }
        }

    private fun PreviewView.findLifecycleOwner(): LifecycleOwner {
        return checkNotNull(ViewTreeLifecycleOwner.get(this)) {
            "Cannot find LifecycleOwner"
        }
    }

    private class Bindings(
        lifecycleOwner: LifecycleOwner,
        val cameraSelector: CameraSelector,
        val preview: Preview?,
        val imageCapture: ImageCapture?,
        val imageAnalysis: ImageAnalysis?
    ) {
        val lifecycleOwner = WeakReference(lifecycleOwner)
    }
}