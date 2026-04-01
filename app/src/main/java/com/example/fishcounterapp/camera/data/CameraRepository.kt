package com.example.fishcounterapp.camera.data

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.fishcounterapp.utils.ProcessingConfig
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraRepository(
    private val context: Context,
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(
        context
    ),
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
) {

    companion object {
        private const val TAG = "CameraRepository"
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrameReceived: (ImageProxy) -> Unit
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(ProcessingConfig.TARGET_WIDTH, ProcessingConfig.TARGET_HEIGHT),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                    Log.d(
                        TAG,
                        "Frame received: ${imageProxy.width}x${imageProxy.height}, " +
                                "format: ${imageProxy.format}, "
                    )
                }

                onFrameReceived(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, mainExecutor)
    }

    fun releaseCamera() {
        try {
            cameraProviderFuture.get().unbindAll()
            cameraExecutor.shutdown()
            Log.d(TAG, "Camera released.")
        } catch (e: Exception) {
            Log.e(TAG, "Camera unbinding failed", e)
        }
    }
}
