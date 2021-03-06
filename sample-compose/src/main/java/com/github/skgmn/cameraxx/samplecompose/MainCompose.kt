package com.github.skgmn.cameraxx.samplecompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skgmn.cameraxx.CameraPreview
import com.github.skgmn.cameraxx.FocusMeteringState
import com.github.skgmn.cameraxx.TorchState
import com.github.skgmn.cameraxx.ZoomState
import com.github.skgmn.startactivityx.PermissionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    permissionStatusFlow: Flow<PermissionStatus>,
    onRequestCameraPermission: () -> Unit,
    onTakePhoto: () -> Unit
) {
    val permissionStatus by permissionStatusFlow.collectAsState(null)
    val permissionInitiallyRequested by viewModel.permissionsInitiallyRequestedState.collectAsState(
        false
    )
    val savingPhoto by viewModel.savingPhotoState.collectAsState()

    if (permissionStatus?.granted == true) {
        CameraLayer(viewModel, onTakePhoto)
    }
    if (permissionInitiallyRequested && permissionStatus?.denied == true) {
        PermissionLayer(onRequestCameraPermission)
    }
    if (savingPhoto) {
        SavingProgress()
    }
}

@Composable
private fun CameraLayer(
    mainViewModel: MainViewModel,
    onTakePhoto: () -> Unit
) {
    val zoomState = remember { ZoomState(pinchZoomEnabled = true) }
    val torchState = remember { TorchState() }
    val focusMeteringState = remember { FocusMeteringState() }

    Box(
        modifier = Modifier
            .background(Color(0xff000000))
            .fillMaxSize()
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            imageCapture = mainViewModel.imageCapture,
            zoomState = zoomState,
            torchState = torchState,
            focusMeteringState = focusMeteringState
        )
        ZoomRatioText(zoomState)
        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            onClick = { onTakePhoto() }
        ) {
            Text(text = stringResource(R.string.take_photo))
        }
        TorchToggleButton(torchState)
    }
}

@Composable
private fun BoxScope.TorchToggleButton(torchState: TorchState) {
    if (torchState.hasFlashUnit == true) {
        torchState.isOn?.let { isOn ->
            Button(
                modifier = Modifier.Companion
                    .align(Alignment.TopEnd)
                    .offset((-8).dp, 8.dp),
                onClick = { torchState.isOn = !isOn }
            ) {
                Text(if (isOn) "Off" else "On")
            }
        }
    }
}

@Composable
fun BoxScope.ZoomRatioText(zoomState: ZoomState) {
    if (zoomState.pinchZoomInProgress && zoomState.ratio != null) {
        Text(
            text = "%.1fx".format(zoomState.ratio),
            color = Color(0xffffffff),
            fontSize = 36.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SavingProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA0000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.saving_photo),
                color = Color(0xffffffff)
            )
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