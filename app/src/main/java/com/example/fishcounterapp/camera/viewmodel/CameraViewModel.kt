package com.example.fishcounterapp.camera.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.domain.processing.FishBlob
import com.example.fishcounterapp.domain.processing.FishTracker
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
import org.opencv.imgproc.Imgproc

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
    val detectedFishCount: Int = 0,
    val totalFishCount: Int = 0
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

    private val fishTracker = FishTracker()

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
        fishTracker.reset()
        _uiState.update { 
            it.copy(
                isBackgroundCaptured = false,
                isSubtractionEnabled = false,
                detectedFishCount = 0,
                totalFishCount = 0
            ) 
        }
    }

    fun toggleSubtraction() {
        if (_uiState.value.isBackgroundCaptured) {
            _uiState.update { 
                val newState = !it.isSubtractionEnabled
                if (!newState) fishTracker.reset()
                it.copy(isSubtractionEnabled = newState) 
            }
        }
    }

    fun resetCount() {
        _uiState.update { it.copy(totalFishCount = 0) }
        fishTracker.reset()
    }

    // --- Frame Processing ---

    fun onFrameReceived(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            var colorMat: Mat? = null
            var grayMat: Mat? = null
            var processedBitmap: Bitmap? = null
            var currentSeenCount = 0
            
            try {
                if (imageProcessor == null) return@launch

                colorMat = ImageConverter.imageProxyToMatDirect(imageProxy)
                if (colorMat == null) return@launch

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

                val currentState = _uiState.value
                val rows = colorMat.rows()
                val lineY = (rows * ProcessingConfig.COUNTING_LINE_Y_PERCENT).toInt()
                
                if (currentState.isSubtractionEnabled) {
                    val maskMat = imageProcessor.subtractBackground(colorMat)
                    if (maskMat != null) {
                        // 1. Detect
                        val detections = imageProcessor.detectFish(maskMat)
                        
                        // 2. Track & Count Crossings
                        val trackedBlobs = fishTracker.update(
                            newDetections = detections,
                            lineY = lineY,
                            onFishCrossed = {
                                viewModelScope.launch(Dispatchers.Main) {
                                    _uiState.update { it.copy(totalFishCount = it.totalFishCount + 1) }
                                }
                            }
                        )
                        currentSeenCount = trackedBlobs.size
                        
                        // 3. Visualization: Show the MASK instead of the raw image
                        // Convert mask to BGR so we can draw colorful overlays on it
                        val visualMat = Mat()
                        Imgproc.cvtColor(maskMat, visualMat, Imgproc.COLOR_GRAY2BGR)
                        
                        imageProcessor.drawCountingLine(visualMat)
                        imageProcessor.drawDetections(visualMat, trackedBlobs)
                        
                        processedBitmap = imageProcessor.matToBitmap(visualMat)
                        
                        visualMat.release()
                        maskMat.release()
                    }
                } else if (currentState.isGrayscaleEnabled) {
                    val displayGrayMat = grayMat ?: imageProcessor.convertToGrayscale(colorMat)
                    processedBitmap = imageProcessor.matToBitmap(displayGrayMat)
                    if (grayMat == null) displayGrayMat.release()
                } else {
                    processedBitmap = imageProcessor.matToBitmap(colorMat)
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            processedBitmap = processedBitmap,
                            detectedFishCount = currentSeenCount
                        )
                    }
                }
                
                updateFpsCounter()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error converting frame", e)
            } finally {
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
