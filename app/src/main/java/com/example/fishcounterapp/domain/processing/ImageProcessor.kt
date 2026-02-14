package com.example.fishcounterapp.domain.processing

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import androidx.core.graphics.createBitmap

class ImageProcessor {
    companion object {
        private const val TAG = "ImageProcessor"

        // Mat type constants for reference
        const val CV_8UC1 = 0   // Grayscale
        const val CV_8UC3 = 16  // BGR
        const val CV_8UC4 = 24  // BGRA
    }

    private var matsCreated = 0
    private var matsReleased = 0

    fun bitmapToMap(bitmap: Bitmap): Mat? {
        return try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            if (!mat.empty()) {
                matsCreated++
                Log.d(TAG, "Mats created: $matsCreated, released: $matsReleased")
                mat
            } else {
                mat.release()
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert bitmap to Mat", e)
            null
        }
    }

    fun onMatReleased() {
        matsReleased++
    }

    fun matToBitmap(mat: Mat): Bitmap? {
        return try {
            if (mat.empty()) {
                Log.e(TAG, "Cannot convert empty Mat to Bitmap")
                return null
            }

            val bitmap = createBitmap(mat.cols(), mat.rows())
            Utils.matToBitmap(mat, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Mat to bitmap", e)
            null
        }
    }

    fun logMatInfo(mat: Mat, label: String = "Mat") {
        Log.d(
            TAG, """
            $label info:
            - Size: ${mat.cols()}x${mat.rows()}
            - Channels: ${mat.channels()}
            - Depth: ${mat.depth()}
            - Type: ${mat.type()}
            - Total elements: ${mat.total()}
            - Is empty: ${mat.empty()}
            - Is continuous: ${mat.isContinuous}
        """.trimIndent()
        )
    }
}