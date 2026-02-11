package com.example.fishcounterapp.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.fishcounterapp.camera.data.CameraRepository

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraRepository: CameraRepository
) {
    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    ) {
        cameraRepository.setupCamera(lifecycleOwner, it)
    }
}
