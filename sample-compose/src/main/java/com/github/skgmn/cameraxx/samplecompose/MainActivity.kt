package com.github.skgmn.cameraxx.samplecompose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.github.skgmn.cameraxx.samplecompose.ui.theme.CameraXXTheme
import com.github.skgmn.startactivityx.*
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXXTheme {
                MainScreen(
                    viewModel,
                    listenPermissionStatus(Manifest.permission.CAMERA),
                    onRequestCameraPermission = {
                        lifecycleScope.launch {
                            requestCameraPermission()
                        }
                    },
                    onTakePhoto = {
                        lifecycleScope.launch {
                            takePhoto()
                        }
                    }
                )
            }
        }

        lifecycleScope.launch {
            requestPermissions(PERMISSIONS)
            viewModel.permissionsInitiallyRequestedState.value = true
        }
    }

    private suspend fun takePhoto() {
        viewModel.savingPhotoState.value = true
        try {
            whenStarted {
                viewModel.takePhotoAsync().await()?.let { uri ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast
                            .makeText(
                                this@MainActivity,
                                R.string.photo_saved,
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        } finally {
            viewModel.savingPhotoState.value = false
        }
    }

    private suspend fun requestCameraPermission() {
        val permissionRequest = PermissionRequest(PERMISSIONS, true)
        requestPermissions(permissionRequest)
    }

    companion object {
        private val PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}