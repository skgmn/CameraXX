package com.github.skgmn.cameraxx.sampledatabinding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.github.skgmn.cameraxx.sampledatabinding.databinding.ActivityMainBinding
import com.github.skgmn.startactivityx.*
import com.github.skgmn.viewmodelevent.handle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.owner = this
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        lifecycleScope.launch {
            requestPermissions(Manifest.permission.CAMERA)
            viewModel.permissionsInitiallyRequested.value = true
            init()
        }
    }

    private fun init() {
        handleEvents()

        lifecycleScope.launch {
            listenPermissionStatus(Manifest.permission.CAMERA).collect {
                viewModel.cameraPermissionsGranted.value = it.granted
            }
        }
    }

    private fun handleEvents() = with(viewModel) {
        handle(requestCameraPermissionsByUserEvent) {
            lifecycleScope.launch {
                val permissionRequest = PermissionRequest(listOf(Manifest.permission.CAMERA), true)
                requestPermissions(permissionRequest)
            }
        }
        handle(requestTakePhotoPermissionsEvent) {
            lifecycleScope.launch {
                val permissionRequest =
                    PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), true)
                val permissionResult = requestPermissions(permissionRequest)
                if (permissionResult.granted) {
                    // ImageCapture seems not work well right after a permission is granted.
                    // In this case recreate and rebind ImageCapture to workaround this issue.
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                        permissionResult == GrantResult.JUST_GRANTED
                    ) {
                        viewModel.replaceImageCapture()
                        binding.executePendingBindings()
                    }
                    whenStarted {
                        viewModel.takePhoto()
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.no_permissions, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        handle(showTakenPhotoEvent) { uri ->
            if (uri != Uri.EMPTY) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    return@handle
                } catch (e: ActivityNotFoundException) {
                    // fall through
                }
            }
            Toast.makeText(this@MainActivity, R.string.photo_saved, Toast.LENGTH_SHORT).show()
        }
    }
}