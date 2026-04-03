package com.example.fishcounterapp.camera.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.domain.processing.FishBlob
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
 */
data class CameraUiState(
    val hasPermission: Boolean = false,
    val isCameraRunning: Boolean = false,
    val errorMessage: String? = null,
    val isOpenCvAvailable: Boolean = false,
    val isGrayscaleEnabled: Boolean = ProcessingConfig.GRAYSCALE_ENABLED_BY_DEFAULT,
    val processedBitmap: Bitmap? = null,
    val currentFps: Int = 0,
    val isBackgroundCaptured: Boolean = false,
    val isCapturingBackground: Boolean = false,
    val isSubtractionEnabled: Boolean = false,
    val detectedFishCount: Int = 0
)

/**
 * ViewModel responsible for managing camera state and image processing orchestration.
 */
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

    // --- Camera & Permission Actions ---

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

    // --- Background Capture & Subtraction Actions ---

    fun requestBackgroundCapture() {
        _uiState.update { it.copy(isCapturingBackground = true) }
    }

    fun clearBackground() {
        imageProcessor?.clearBackground()
        _uiState.update { 
            it.copy(
                isBackgroundCaptured = false,
                isSubtractionEnabled = false,
                detectedFishCount = 0
            ) 
        }
    }

    fun toggleSubtraction() {
        if (_uiState.value.isBackgroundCaptured) {
            _uiState.update { it.copy(isSubtractionEnabled = !it.isSubtractionEnabled) }
        }
    }

    // --- Frame Processing ---

    fun onFrameReceived(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var colorMat: Mat? = null
            var grayMat: Mat? = null
            var processedBitmap: Bitmap? = null
            var fishCount = 0
            
            try {
                if (imageProcessor == null) return@launch

                // 1. Convert ImageProxy to Mat
                colorMat = ImageConverter.imageProxyToMatDirect(imageProxy)
                if (colorMat == null) return@launch

                // 2. Handle Background Capture if requested
                if (_uiState.value.isCapturingBackground) {
                    grayMat = imageProcessor.convertToGrayscale(colorMat)
                    imageProcessor.setBackground(grayMat)
                    
                    withContext(Dispatchers.Main) {
                        _uiState.update { 
                            it.copy(
                                isCapturingBackground = false,
                                isBackgroundCaptured = true
                            ) 
                        }
                    }
                }

                // 3. Process the Mat for display and detection
                val currentState = _uiState.value
                
                if (currentState.isSubtractionEnabled) {
                    // Perform Background Subtraction
                    val maskMat = imageProcessor.subtractBackground(colorMat)
                    if (maskMat != null) {
                        // Detect Fish Blobs
                        val blobs = imageProcessor.detectFish(maskMat)
                        fishCount = blobs.size
                        
                        // Draw Detections on color frame for feedback
                        imageProcessor.drawDetections(colorMat, blobs)
                        
                        // Decide what to display: Mask or Color with Detections?
                        // For now, let's show color with detections to verify it works
                        processedBitmap = imageProcessor.matToBitmap(colorMat)
                        
                        maskMat.release()
                    }
                } else if (currentState.isGrayscaleEnabled) {
                    val displayGrayMat = grayMat ?: imageProcessor.convertToGrayscale(colorMat)
                    processedBitmap = imageProcessor.matToBitmap(displayGrayMat)
                    if (grayMat == null) displayGrayMat.release()
                } else {
                    processedBitmap = imageProcessor.matToBitmap(colorMat)
                }

                // 4. Update UI
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            processedBitmap = processedBitmap,
                            detectedFishCount = fishCount
                        )
                    }
                }
                
                updateFpsCounter()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error converting frame", e)
            } finally {
                // 5. Resource Cleanup
                colorMat?.release()
                grayMat?.release()
                imageProxy.close()
            }
        }
    }

    // --- Performance Tracking ---

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()

    private fun updateFpsCounter() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= ProcessingConfig.FPS_UPDATE_INTERVAL_MS) {
            val fps = (frameCount * 1000) / elapsed
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { it.copy(currentFps = fps.toInt()) }
            }
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}
