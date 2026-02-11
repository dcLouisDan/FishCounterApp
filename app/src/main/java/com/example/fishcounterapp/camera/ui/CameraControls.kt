package com.example.fishcounterapp.camera.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    isCameraRunning: Boolean,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = {
            if (isCameraRunning) {
                onStopCamera()
            } else {
                onStartCamera()
            }
        }
    ) {
        Text(text = if (isCameraRunning) "Stop Camera" else "Start Camera")
    }
}
