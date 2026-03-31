package com.example.fishcounterapp.camera.ui

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    isRunning: Boolean = false,
    currentFps: Int,
    processedBitmap: Bitmap? = null
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (processedBitmap != null) {
            ProcessedImageView(bitmap = processedBitmap, modifier = Modifier.fillMaxSize())
        }

        if (isRunning) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "FPS: $currentFps",
                    modifier = Modifier.padding(8.dp),
                    color = if (currentFps >= 15) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

}
