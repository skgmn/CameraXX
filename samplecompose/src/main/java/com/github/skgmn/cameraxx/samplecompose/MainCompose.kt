package com.github.skgmn.cameraxx.samplecompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.skgmn.cameraxx.CameraPreview
import com.github.skgmn.startactivityx.PermissionStatus
import kotlinx.coroutines.flow.Flow

@Composable
fun MainScreen(
    permissionStatusFlow: Flow<PermissionStatus>,
    onRequestCameraPermission: () -> Unit,
    onTakePhoto: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()

    val permissionStatus by permissionStatusFlow.collectAsState(null)
    val imageCapture by viewModel.imageCaptureUseCase.collectAsState(null)
    val permissionInitiallyRequested by viewModel.permissionsInitiallyRequested.collectAsState(
        false
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xff000000))
    ) {
        if (permissionStatus?.granted == true) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture
            )
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                onClick = { onTakePhoto() }
            ) {
                Text(text = stringResource(R.string.take_photo))
            }
        }
        if (permissionInitiallyRequested && permissionStatus?.denied == true) {
            PermissionLayer(onRequestCameraPermission)
        }
    }
}

@Composable
private fun PermissionLayer(onRequestCameraPermission: () -> Unit) {
    val currentOnRequestCameraPermission by rememberUpdatedState(onRequestCameraPermission)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff000000))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.permissions_required),
                color = colorResource(android.R.color.white),
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { currentOnRequestCameraPermission() }) {
                Text(
                    text = stringResource(id = R.string.grant_permissions),
                    fontSize = 13.sp
                )
            }
        }
    }
}