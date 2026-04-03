package com.example.fishcounterapp.domain.processing

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import androidx.core.graphics.createBitmap
import com.example.fishcounterapp.utils.ProcessingConfig
import org.opencv.core.Core
import org.opencv.core.Size
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
        
        // Apply Gaussian Blur to the background reference once to stabilize it
        val blurredBg = Mat()
        val blurSize = ProcessingConfig.GAUSSIAN_BLUR_SIZE
        Imgproc.GaussianBlur(mat, blurredBg, Size(blurSize, blurSize), 0.0)
        
        backgroundMat = blurredBg
        
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(TAG, "Background reference captured and blurred. Size: ${mat.cols()}x${mat.rows()}")
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
        val blurredFrame = Mat()
        val diffMat = Mat()
        val maskMat = Mat()

        try {
            // 1. Pre-process current frame: Blur to reduce high-frequency noise (wobble)
            val blurSize = ProcessingConfig.GAUSSIAN_BLUR_SIZE
            Imgproc.GaussianBlur(grayFrame, blurredFrame, Size(blurSize, blurSize), 0.0)

            // 2. Absolute difference against the pre-blurred background
            Core.absdiff(bg, blurredFrame, diffMat)

            // 3. Thresholding to create binary mask
            Imgproc.threshold(
                diffMat, 
                maskMat, 
                ProcessingConfig.SUBTRACTION_THRESHOLD, 
                ProcessingConfig.BINARY_MAX_VALUE, 
                Imgproc.THRESH_BINARY
            )

            // 4. Median Blur on the mask to remove "salt and pepper" noise
            // This is very effective for simulated wobbling noise.
            Imgproc.medianBlur(maskMat, maskMat, ProcessingConfig.MEDIAN_BLUR_SIZE)

            // 5. Morphological enhancement
            val kernelSize = ProcessingConfig.MORPH_KERNEL_SIZE
            val kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, 
                Size(kernelSize, kernelSize)
            )
            
            // OPEN to remove any remaining tiny noise
            Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_OPEN, kernel, Point(-1.0, -1.0), ProcessingConfig.MORPH_OPEN_ITERATIONS)
            
            // DILATE slightly to merge parts of the same fish without white-out
            if (ProcessingConfig.MORPH_DILATE_ITERATIONS > 0) {
                Imgproc.dilate(maskMat, maskMat, kernel, Point(-1.0, -1.0), ProcessingConfig.MORPH_DILATE_ITERATIONS)
            }
            
            kernel.release()

            return maskMat
        } catch (e: Exception) {
            Log.e(TAG, "Background subtraction failed", e)
            maskMat.release()
            return null
        } finally {
            grayFrame.release()
            blurredFrame.release()
            diffMat.release()
        }
    }

    /**
     * Detects fish blobs in the provided binary mask.
     */
    fun detectFish(mask: Mat): List<FishBlob> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        val fishBlobs = mutableListOf<FishBlob>()

        try {
            // Find all contours in the mask
            Imgproc.findContours(
                mask, 
                contours, 
                hierarchy, 
                Imgproc.RETR_EXTERNAL, 
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                
                // Filter by area to exclude noise and large artifacts
                if (area >= ProcessingConfig.MIN_FISH_AREA && area <= ProcessingConfig.MAX_FISH_AREA) {
                    val moments = Imgproc.moments(contour)
                    // Ensure area is not zero to avoid division by zero
                    if (moments._m00 != 0.0) {
                        val centerX = moments._m10 / moments._m00
                        val centerY = moments._m01 / moments._m00
                        
                        val rect = Imgproc.boundingRect(contour)
                        
                        fishBlobs.add(
                            FishBlob(
                                center = Point(centerX, centerY),
                                boundingBox = rect,
                                area = area
                            )
                        )
                    }
                }
                contour.release() // Release each contour mat
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fish detection failed", e)
        } finally {
            hierarchy.release()
        }

        return fishBlobs
    }

    /**
     * Draws detection overlays (bounding boxes, centers, and IDs) on the frame.
     */
    fun drawDetections(frame: Mat, blobs: List<FishBlob>) {
        val color = Scalar(0.0, 255.0, 0.0) // Green
        val thickness = 2

        for (blob in blobs) {
            // Draw bounding box
            Imgproc.rectangle(frame, blob.boundingBox.tl(), blob.boundingBox.br(), color, thickness)
            
            // Draw center point
            Imgproc.circle(frame, blob.center, 4, color, -1)

            // Draw ID if available
            if (blob.id != -1) {
                Imgproc.putText(
                    frame,
                    "ID: ${blob.id}",
                    Point(blob.boundingBox.x.toDouble(), (blob.boundingBox.y - 10).toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    color,
                    1
                )
            }
        }
    }

    /**
     * Draws the counting line on the frame.
     */
    fun drawCountingLine(frame: Mat) {
        val lineY = (frame.rows() * ProcessingConfig.COUNTING_LINE_Y_PERCENT).toInt()
        val startPoint = Point(0.0, lineY.toDouble())
        val endPoint = Point(frame.cols().toDouble(), lineY.toDouble())
        
        // Draw a blue line
        Imgproc.line(frame, startPoint, endPoint, Scalar(255.0, 0.0, 0.0), 2)
        
        // Add text label
        Imgproc.putText(
            frame, 
            "Counting Line", 
            Point(10.0, (lineY - 10).toDouble()), 
            Imgproc.FONT_HERSHEY_SIMPLEX, 
            0.5, 
            Scalar(255.0, 0.0, 0.0), 
            1
        )
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
