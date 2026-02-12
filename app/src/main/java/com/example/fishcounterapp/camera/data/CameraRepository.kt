package com.example.fishcounterapp.camera.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import androidx.core.graphics.createBitmap
import java.util.concurrent.Executors

class CameraRepository(
    private val context: Context,
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(
        context
    ),
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = imageProxyToBitmap(imageProxy)
                // TODO: Process the bitmap here

                imageProxy.close()
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
                Log.e("CameraRepository", "Camera binding failed", e)
            }
        }, mainExecutor)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        // TODO: Convert ImageProxy to Bitmap
        return createBitmap(imageProxy.width, imageProxy.height)
    }

    fun releaseCamera() {
        try {
            cameraProviderFuture.get().unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e("CameraRepository", "Camera unbinding failed", e)
        }
    }
}
