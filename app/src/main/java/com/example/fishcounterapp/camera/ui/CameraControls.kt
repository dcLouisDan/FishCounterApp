package com.example.fishcounterapp.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
    onToggleGrayscale: () -> Unit,
    isBackgroundCaptured: Boolean,
    onCaptureBackground: () -> Unit,
    onClearBackground: () -> Unit,
    isSubtractionEnabled: Boolean,
    onToggleSubtraction: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleGrayscale,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubtractionEnabled // Disable grayscale toggle if subtraction is on (since it's already mask)
                ) {
                    Text(if (isGrayscaleEnabled) "Show Color" else "Show Grayscale")
                }

                Button(
                    onClick = onToggleSubtraction,
                    modifier = Modifier.weight(1f),
                    enabled = isBackgroundCaptured,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubtractionEnabled) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isSubtractionEnabled) "Stop Subtraction" else "Start Subtraction")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCaptureBackground,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBackgroundCaptured) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isBackgroundCaptured) "Retake Background" else "Capture Background")
                }

                if (isBackgroundCaptured) {
                    OutlinedButton(
                        onClick = onClearBackground,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear BG")
                    }
                }
            }
        }
    }
}