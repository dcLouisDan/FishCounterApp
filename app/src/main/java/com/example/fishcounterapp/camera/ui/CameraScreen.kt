package com.example.fishcounterapp.camera.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.camera.viewmodel.CameraViewModel
import com.example.fishcounterapp.camera.viewmodel.CameraViewModelFactory

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = viewModel(
        factory = CameraViewModelFactory(
            CameraRepository(LocalContext.current)
        )
    )
) {
    val uiState by cameraViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            cameraViewModel.onPermissionResult(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        if (!uiState.hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.hasPermission) {
            if (uiState.isCameraRunning) {
                CameraPreview(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    cameraRepository = CameraRepository(context)
                )
            }
            CameraControls(
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(bottom = 16.dp),
                isCameraRunning = uiState.isCameraRunning,
                onStartCamera = {
                    cameraViewModel.startCamera()
                },
                onStopCamera = { cameraViewModel.stopCamera() }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "This App requires camera permission in order to function.")
                CameraControls(
                    isCameraRunning = false,
                    onStartCamera = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onStopCamera = {}
                )
            }
        }

        uiState.errorMessage?.let {
            Text(text = it, modifier = Modifier.align(Alignment.Center))
        }
    }
}
