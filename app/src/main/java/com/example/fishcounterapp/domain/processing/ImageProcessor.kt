package com.example.fishcounterapp.domain.processing

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import androidx.core.graphics.createBitmap
import com.example.fishcounterapp.utils.ProcessingConfig
import org.opencv.imgproc.Imgproc

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
                if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                    Log.d(TAG, "Mats created: $matsCreated, released: $matsReleased")
                }
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

            val bitmap = if (mat.channels() == 1) {
                val bgraMat = Mat()
                Imgproc.cvtColor(mat, bgraMat, Imgproc.COLOR_GRAY2BGRA)
                val resultBitmap =
                    createBitmap(bgraMat.cols(), bgraMat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(bgraMat, resultBitmap)
                bgraMat.release()
                resultBitmap
            } else {
                val resultBitmap = createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(mat, resultBitmap)
                resultBitmap
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Mat to bitmap", e)
            null
        }
    }

    fun logMatInfo(mat: Mat, label: String = "Mat") {
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(
                TAG, """
                $label info:
                - Size: ${mat.cols()}x${mat.rows()}
                - Channels: ${mat.channels()}
                - Depth: ${mat.depth()}
                - Type: ${mat.type()}
                - Total elements: ${mat.total()}
                - Is empty: ${mat.empty()}
                - Is continuous: ${mat.isContinuous}
            """.trimIndent()
            )
        }
    }

    fun convertToGrayscale(colorMat: Mat): Mat {
        val grayMat = Mat()

        when (colorMat.channels()) {
            4 -> {
                // BGRA (4 channels) -> Gray
                Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
                if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                    Log.d(TAG, "Converted BGRA to Grayscale")
                }
            }

            3 -> {
                // BGR (3 channels) -> Gray
                Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)
                if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                    Log.d(TAG, "Converted BGR to Grayscale")
                }
            }

            1 -> {
                // Already grayscale
                if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
                    Log.d(TAG, "Mat already Grayscale")
                }
                return colorMat.clone()
            }

            else -> {
                Log.e(TAG, "Unsupported channel count: ${colorMat.channels()}")
                return colorMat.clone()
            }
        }

        return grayMat
    }
}