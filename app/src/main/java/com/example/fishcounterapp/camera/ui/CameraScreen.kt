package com.example.fishcounterapp.camera.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.camera.viewmodel.CameraViewModel
import com.example.fishcounterapp.utils.cameraViewModel

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier, cameraViewModel: CameraViewModel = cameraViewModel()
) {
    val context = LocalContext.current
    val uiState by cameraViewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(), onResult = { isGranted ->
            cameraViewModel.onPermissionResult(isGranted)
        })


    LaunchedEffect(Unit) {
        if (!uiState.hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.hasPermission) {
            CameraPreview(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                cameraRepository = CameraRepository(context),
                viewModel = cameraViewModel,
                isRunning = uiState.isCameraRunning,
                processedBitmap = uiState.processedBitmap,
                currentFps = uiState.currentFps
            )

            // Top Info Bar
            if (uiState.isCameraRunning) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Count Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL FISH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${uiState.totalFishCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Current Seen Count
                    if (uiState.isSubtractionEnabled) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Currently in view: ${uiState.detectedFishCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Status Indicators (Top Right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (uiState.isCameraRunning && uiState.isBackgroundCaptured) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.8f), // Green status
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "BG READY",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                if (uiState.isSubtractionEnabled) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "COUNTING ACTIVE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (!uiState.isCameraRunning) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome to the Fish-Counter App")
                        Text("Press 'Start Camera' to begin counting fish")
                    }
                }
            }

            CameraControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                isCameraRunning = uiState.isCameraRunning,
                onStartCamera = {
                    cameraViewModel.startCamera()
                },
                onStopCamera = { cameraViewModel.stopCamera() },
                isGrayscaleEnabled = uiState.isGrayscaleEnabled,
                onToggleGrayscale = {
                    cameraViewModel.toggleGrayscale()
                },
                isBackgroundCaptured = uiState.isBackgroundCaptured,
                onCaptureBackground = {
                    cameraViewModel.requestBackgroundCapture()
                },
                onClearBackground = {
                    cameraViewModel.clearBackground()
                },
                isSubtractionEnabled = uiState.isSubtractionEnabled,
                onToggleSubtraction = {
                    cameraViewModel.toggleSubtraction()
                },
                onResetCount = {
                    cameraViewModel.resetCount()
                }
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
                    onStopCamera = {},
                    isGrayscaleEnabled = uiState.isGrayscaleEnabled,
                    onToggleGrayscale = {
                        cameraViewModel.toggleGrayscale()
                    },
                    isBackgroundCaptured = false,
                    onCaptureBackground = {},
                    onClearBackground = {},
                    isSubtractionEnabled = false,
                    onToggleSubtraction = {},
                    onResetCount = {}
                )
            }
        }

        uiState.errorMessage?.let {
            Text(text = it, modifier = Modifier.align(Alignment.Center))
        }
    }
}
