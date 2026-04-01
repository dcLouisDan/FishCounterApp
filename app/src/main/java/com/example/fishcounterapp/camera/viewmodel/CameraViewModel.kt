package com.example.fishcounterapp.camera.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.domain.processing.ImageProcessor
import com.example.fishcounterapp.utils.ImageConverter
import com.example.fishcounterapp.utils.ProcessingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat

/**
 * UI State for the Camera screen.
 *
 * @property hasPermission Whether camera permission has been granted.
 * @property isCameraRunning Whether the camera is currently active.
 * @property errorMessage Optional error message to display to the user.
 * @property isOpenCvAvailable Whether OpenCV has been successfully initialized.
 * @property isGrayscaleEnabled Whether grayscale processing is currently active.
 * @property processedBitmap The latest processed frame to be displayed.
 * @property currentFps Current frames per second of the processing pipeline.
 */
data class CameraUiState(
    val hasPermission: Boolean = false,
    val isCameraRunning: Boolean = false,
    val errorMessage: String? = null,
    val isOpenCvAvailable: Boolean = false,
    val isGrayscaleEnabled: Boolean = ProcessingConfig.GRAYSCALE_ENABLED_BY_DEFAULT,
    val processedBitmap: Bitmap? = null,
    val currentFps: Int = 0
)

/**
 * ViewModel responsible for managing camera state and image processing orchestration.
 *
 * This ViewModel handles:
 * 1. Camera lifecycle and permission states.
 * 2. Receiving frames from CameraX.
 * 3. Routing frames through [ImageProcessor] and [ImageConverter].
 * 4. Maintaining UI state for the camera preview and processing results.
 */
class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val imageProcessor: ImageProcessor?,
    val isOpenCvInitialized: Boolean
) : ViewModel() {

    companion object {
        private const val TAG = "CameraViewModel"
    }

    // --- State Management ---

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

    // --- Camera & Permission Actions ---

    /**
     * Handles the result of the camera permission request.
     */
    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasPermission = isGranted) }
        if (!isGranted) {
            _uiState.update { it.copy(errorMessage = "Camera permission denied.") }
        }
    }

    /**
     * Starts the camera session.
     */
    fun startCamera() {
        _uiState.update { it.copy(isCameraRunning = true) }
    }

    /**
     * Stops the camera session and releases resources.
     */
    fun stopCamera() {
        _uiState.update { it.copy(isCameraRunning = false) }
        cameraRepository.releaseCamera()
    }

    /**
     * Toggles the grayscale processing filter.
     */
    fun toggleGrayscale() {
        _uiState.update {
            it.copy(
                isGrayscaleEnabled = !it.isGrayscaleEnabled
            )
        }
    }

    // --- Frame Processing ---

    /**
     * Processes a new frame received from the camera.
     *
     * This method:
     * 1. Converts [ImageProxy] to OpenCV [Mat].
     * 2. Applies optional grayscale filter.
     * 3. Converts [Mat] back to [Bitmap] for UI display.
     * 4. Updates FPS tracking.
     */
    fun onFrameReceived(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var colorMat: Mat? = null
            var grayMat: Mat? = null
            var processedBitmap: Bitmap? = null
            
            try {
                if (imageProcessor == null) {
                    if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                        Log.w(TAG, "Failed to convert frame to bitmap")
                    }
                    return@launch
                }

                // 1. Convert ImageProxy to Mat
                colorMat = ImageConverter.imageProxyToMatDirect(imageProxy)
                if (colorMat == null) return@launch

                // 2. Process the Mat
                if (_uiState.value.isGrayscaleEnabled) {
                    grayMat = imageProcessor.convertToGrayscale(colorMat)
                    processedBitmap = imageProcessor.matToBitmap(grayMat)

                    val processingTime = System.currentTimeMillis() - startTime
                    if (ProcessingConfig.LOG_FRAME_TIMING) {
                        Log.d(TAG, "Grayscale processed: ${processingTime}ms")
                    }
                } else {
                    processedBitmap = imageProcessor.matToBitmap(colorMat)
                    val processingTime = System.currentTimeMillis() - startTime
                    if (ProcessingConfig.LOG_FRAME_TIMING) {
                        Log.d(TAG, "Color processed: ${processingTime}ms")
                    }
                }

                // 3. Update UI
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(processedBitmap = processedBitmap)
                    }
                }
                
                updateFpsCounter()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error converting frame", e)
            } finally {
                // 4. Resource Cleanup
                if (colorMat != null) {
                    colorMat.release()
                    imageProcessor?.onMatReleased()
                }
                grayMat?.release()
                imageProxy.close()
            }
        }
    }

    // --- Performance Tracking ---

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()

    /**
     * Updates the FPS counter based on the time elapsed since the last second.
     */
    private fun updateFpsCounter() {
        frameCount++

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= ProcessingConfig.FPS_UPDATE_INTERVAL_MS) {
            val fps = (frameCount * 1000) / elapsed

            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { it.copy(currentFps = fps.toInt()) }
            }

            if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                Log.d(TAG, "FPS: $fps")
            }

            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}
