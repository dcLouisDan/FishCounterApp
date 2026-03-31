package com.example.fishcounterapp.camera.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.domain.processing.ImageProcessor
import com.example.fishcounterapp.utils.ImageConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat

data class CameraUiState(
    val hasPermission: Boolean = false,
    val isCameraRunning: Boolean = false,
    val errorMessage: String? = null,
    val isOpenCvAvailable: Boolean = false,
    val isGrayscaleEnabled: Boolean = false,
    val processedBitmap: Bitmap? = null,
    val currentFps: Int = 0
)

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val imageProcessor: ImageProcessor?,
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

    fun toggleGrayscale() {
        _uiState.update {
            it.copy(
                isGrayscaleEnabled = !it.isGrayscaleEnabled
            )
        }
    }

    fun onFrameReceived(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var colorMat: Mat? = null
            var grayMat: Mat? = null
            var processedBitmap: Bitmap? = null
            try {
                val bitmap = ImageConverter.imageProxyToBitmap(imageProxy)
                if (bitmap == null || imageProcessor == null) {
                    Log.w(TAG, "Failed to convert frame to bitmap")
                    return@launch
                }
                colorMat = imageProcessor.bitmapToMap(bitmap)
                if (colorMat == null) return@launch
                imageProcessor.logMatInfo(colorMat, "Original")
                if (_uiState.value.isGrayscaleEnabled) {
                    val grayStartTime = System.currentTimeMillis()
                    grayMat = imageProcessor.convertToGrayscale(colorMat)

                    processedBitmap = imageProcessor.matToBitmap(grayMat)

                    val processingTime = System.currentTimeMillis() - startTime

                    Log.d(
                        TAG,
                        "Grayscale processed and converted to Bitmap, " +
                                "time: ${processingTime}ms"
                    )
                } else {
                    processedBitmap = bitmap
                    val processingTime = System.currentTimeMillis() - startTime

                    Log.d(
                        TAG,
                        "Color: ${colorMat.cols()}x${colorMat.rows()}, " +
                                "time: ${processingTime}ms"
                    )
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            processedBitmap = processedBitmap
                        )
                    }
                }
                updateFpsCounter()
            } catch (e: Exception) {
                Log.e(TAG, "Error converting frame", e)
            } finally {
                if (colorMat !== null) {
                    colorMat.release()
                    imageProcessor?.onMatReleased()
                }
                grayMat?.release()
                imageProxy.close()
            }
        }
    }

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private fun updateFpsCounter() {
        frameCount++

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= 1000) {
            val fps = (frameCount * 1000) / elapsed

            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { it.copy(currentFps = fps.toInt()) }
            }

            Log.d(TAG, "FPS: $fps")

            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}