package com.example.fishcounterapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fishcounterapp.ui.theme.FishCounterAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FishCounterAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraActive by remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasCameraPermission) {
            Text(
                text = "This App requires camera permission in order to function.",
            )
            Button(onClick = {
                launcher.launch(Manifest.permission.CAMERA)
            }) {
                Text(text = "Grant Camera Permission")
            }
        } else {
            Text(text = if (isCameraActive) "Camera Active" else "Camera Inactive")
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreviewWindow(isCameraActive = isCameraActive)
                ToggleCameraButton(isCameraActive = isCameraActive, onToggle = {
                    isCameraActive = !isCameraActive
                }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun ToggleCameraButton(
    modifier: Modifier = Modifier,
    isCameraActive: Boolean = true,
    onToggle: () -> Unit
) {
    Button(modifier = modifier, onClick = onToggle) {
        Text(text = if (isCameraActive) "Stop Camera" else "Start Camera")
    }
}

@Composable
fun CameraPreviewWindow(modifier: Modifier = Modifier, isCameraActive: Boolean = true) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreviewWindow", "Unbinding Failed", e)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { previewView }
    ) { _ ->
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            if (isCameraActive) {
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreviewWindow", "Binding Failed", e)
                }
            } else {
                cameraProvider.unbindAll()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FishCounterAppTheme {
        MainScreen()
    }
}