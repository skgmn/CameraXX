package com.github.skgmn.cameraxx.samplecompose

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.skgmn.cameraxx.takePicture
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _imageCaptureState = MutableStateFlow(newImageCapture())

    val preview = Preview.Builder().build()
    val imageCaptureState: StateFlow<ImageCapture> = _imageCaptureState

    val permissionsInitiallyRequestedState = MutableStateFlow(false)
    val savingPhotoState = MutableStateFlow(false)

    fun takePhotoAsync(): Deferred<Uri?> {
        return viewModelScope.async {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            ).build()

            _imageCaptureState.value.takePicture(outputOptions).savedUri
        }
    }

    fun replaceImageCapture() {
        _imageCaptureState.value = newImageCapture()
    }

    private fun newImageCapture() = ImageCapture.Builder().build()
}