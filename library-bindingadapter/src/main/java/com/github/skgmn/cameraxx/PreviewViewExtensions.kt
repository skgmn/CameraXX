package com.github.skgmn.cameraxx

import androidx.camera.view.PreviewView
import com.github.skgmn.cameraxx.bindingadapter.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

fun PreviewView.getCamera(): Flow<Camera?> {
    @Suppress("UNCHECKED_CAST")
    return getTag(R.id.previewViewCameraFlow) as? MutableStateFlow<Camera?>
        ?: MutableStateFlow<Camera?>(null).also {
            setTag(R.id.previewViewCameraFlow, it)
        }
}