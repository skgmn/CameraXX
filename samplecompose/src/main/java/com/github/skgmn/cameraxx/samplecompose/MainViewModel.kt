package com.github.skgmn.cameraxx.samplecompose

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.skgmn.cameraxx.takePicture
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _imageCaptureUseCase = MutableStateFlow(newImageCapture())

    val imageCaptureUseCase: StateFlow<ImageCapture> = _imageCaptureUseCase

    val permissionsInitiallyRequested = MutableStateFlow(false)

    fun takePhotoAsync(): Deferred<Uri?> {
        return viewModelScope.async {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            ).build()

            _imageCaptureUseCase.value.takePicture(outputOptions).savedUri
        }
    }

    fun replaceImageCapture() {
        _imageCaptureUseCase.value = newImageCapture()
    }

    private fun newImageCapture() = ImageCapture.Builder().build()
}