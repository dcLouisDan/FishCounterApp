package com.example.fishcounterapp.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.camera.viewmodel.CameraViewModel

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraRepository: CameraRepository,
    viewModel: CameraViewModel,
    isRunning: Boolean = false
) {

    DisposableEffect(isRunning) {
        if (isRunning) {
            cameraRepository.setupCamera(
                lifecycleOwner,
                previewView,
                onFrameReceived = { imageProxy ->
                    viewModel.onFrameReceived(imageProxy)
                })
        }

        onDispose {
            cameraRepository.releaseCamera()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
