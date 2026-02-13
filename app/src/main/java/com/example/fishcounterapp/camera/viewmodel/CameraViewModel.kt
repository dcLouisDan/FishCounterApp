package com.example.fishcounterapp.camera.viewmodel

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.fishcounterapp.camera.data.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraUiState(
    val hasPermission: Boolean = false,
    val isCameraRunning: Boolean = false,
    val errorMessage: String? = null,
    val isOpenCvAvailable: Boolean = false
)

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    val isOpenCvInitialized: Boolean
) : ViewModel() {

    companion object {
        private const val TAG = "CameraViewModel"
    }

    private val _uiState = MutableStateFlow(
        CameraUiState(
            isOpenCvAvailable = isOpenCvInitialized
        )
    )
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()

    init {
        if (!isOpenCvInitialized) {
            Log.w(TAG, "OpenCV is not initialized. Camera features may not work.")
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasPermission = isGranted) }
        if (!isGranted) {
            _uiState.update { it.copy(errorMessage = "Camera permission denied.") }
        }
    }

    fun startCamera() {
        _uiState.update { it.copy(isCameraRunning = true) }
    }

    fun stopCamera() {
        _uiState.update { it.copy(isCameraRunning = false) }
        cameraRepository.releaseCamera()
    }

    fun onFrameReceived(imageProxy: ImageProxy) {
        frameCount++

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= 1000) {
            val fps = (frameCount * 1000) / elapsed
            Log.d(TAG, "FPS: $fps, Frame received: ${imageProxy.width}x${imageProxy.height}")

            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}