package com.example.fishcounterapp.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    isCameraRunning: Boolean,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    isGrayscaleEnabled: Boolean,
    onToggleGrayscale: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        if (isCameraRunning) {
            OutlinedButton(
                onClick = onToggleGrayscale,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isGrayscaleEnabled) "Show Color" else "Show Grayscale")
            }
        }
    }
}
