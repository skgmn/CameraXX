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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val imageCapture = ImageCapture.Builder().build()

    val permissionsInitiallyRequestedState = MutableStateFlow(false)
    val savingPhotoState = MutableStateFlow(false)

    fun takePhotoAsync(): Deferred<Uri?> {
        return viewModelScope.async {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            ).build()

            imageCapture.takePicture(outputOptions).savedUri
        }
    }

    private fun newImageCapture() = ImageCapture.Builder().build()
}