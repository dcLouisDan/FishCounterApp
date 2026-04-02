package com.example.fishcounterapp.domain.processing

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import androidx.core.graphics.createBitmap
import com.example.fishcounterapp.utils.ProcessingConfig
import org.opencv.core.Core
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

    private var backgroundMat: Mat? = null

    /**
     * Captures and stores a copy of the provided Mat as the background reference.
     * Expects a grayscale Mat.
     */
    fun setBackground(mat: Mat) {
        backgroundMat?.release()
        backgroundMat = mat.clone()
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(TAG, "Background reference captured. Size: ${mat.cols()}x${mat.rows()}")
        }
    }

    /**
     * Returns the current background reference Mat.
     */
    fun getBackground(): Mat? = backgroundMat

    /**
     * Releases the background reference memory.
     */
    fun clearBackground() {
        backgroundMat?.release()
        backgroundMat = null
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(TAG, "Background reference cleared")
        }
    }

    /**
     * Checks if a valid background reference is currently stored.
     */
    fun hasBackground(): Boolean = backgroundMat != null && !backgroundMat!!.empty()

    /**
     * Subtracts the stored background from the current frame.
     * @param currentFrame The frame to process (color or grayscale).
     * @return A binary mask where movement is detected, or null if no background is set.
     */
    fun subtractBackground(currentFrame: Mat): Mat? {
        val bg = backgroundMat ?: return null
        if (bg.empty()) return null

        val grayFrame = convertToGrayscale(currentFrame)
        val diffMat = Mat()
        val maskMat = Mat()

        try {
            // 1. Absolute difference
            Core.absdiff(bg, grayFrame, diffMat)

            // 2. Thresholding to create binary mask
            // Using configurable threshold values
            Imgproc.threshold(
                diffMat, 
                maskMat, 
                ProcessingConfig.SUBTRACTION_THRESHOLD, 
                ProcessingConfig.BINARY_MAX_VALUE, 
                Imgproc.THRESH_BINARY
            )

            // 3. Optional: Noise reduction (Dilation then Erosion - Closing)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, org.opencv.core.Size(3.0, 3.0))
            Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_OPEN, kernel)
            kernel.release()

            return maskMat
        } catch (e: Exception) {
            Log.e(TAG, "Background subtraction failed", e)
            maskMat.release()
            return null
        } finally {
            grayFrame.release()
            diffMat.release()
        }
    }

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
        if (colorMat.channels() == 1) return colorMat.clone()
        
        val grayMat = Mat()
        when (colorMat.channels()) {
            4 -> Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
            3 -> Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)
            else -> {
                Log.e(TAG, "Unsupported channel count for grayscale: ${colorMat.channels()}")
                return colorMat.clone()
            }
        }
        return grayMat
    }
}