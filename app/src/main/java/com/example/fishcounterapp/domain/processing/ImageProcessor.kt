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
        
        val blurSize = ProcessingConfig.GAUSSIAN_BLUR_SIZE
        if (blurSize > 0) {
            val blurredBg = Mat()
            Imgproc.GaussianBlur(mat, blurredBg, Size(blurSize, blurSize), 0.0)
            backgroundMat = blurredBg
        } else {
            backgroundMat = mat.clone()
        }
        
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(TAG, "Background reference captured. Blur: $blurSize")
        }
    }

    fun getBackground(): Mat? = backgroundMat

    fun clearBackground() {
        backgroundMat?.release()
        backgroundMat = null
        if (ProcessingConfig.ENABLE_VERBOSE_LOGGING) {
            Log.d(TAG, "Background reference cleared")
        }
    }

    fun hasBackground(): Boolean = backgroundMat != null && !backgroundMat!!.empty()

    /**
     * Subtracts the stored background from the current frame and applies cleanup.
     */
    fun subtractBackground(currentFrame: Mat): Mat? {
        val bg = backgroundMat ?: return null
        if (bg.empty()) return null

        val grayFrame = convertToGrayscale(currentFrame)
        val diffMat = Mat()
        val maskMat = Mat()

        try {
            // 1. Pre-process: Blur (Only if size > 0)
            val blurSize = ProcessingConfig.GAUSSIAN_BLUR_SIZE
            val frameToSubtract = if (blurSize > 0) {
                val blurred = Mat()
                Imgproc.GaussianBlur(grayFrame, blurred, Size(blurSize, blurSize), 0.0)
                blurred
            } else {
                grayFrame
            }

            // 2. Diff
            Core.absdiff(bg, frameToSubtract, diffMat)

            // 3. Threshold
            Imgproc.threshold(
                diffMat, 
                maskMat, 
                ProcessingConfig.SUBTRACTION_THRESHOLD, 
                ProcessingConfig.BINARY_MAX_VALUE, 
                Imgproc.THRESH_BINARY
            )

            // Cleanup intermediate blurred frame if created
            if (frameToSubtract !== grayFrame) {
                frameToSubtract.release()
            }

            // 4. Region of Interest (ROI) - Black out the edges
            applyROIMask(maskMat)

            // 5. Median Blur
            if (ProcessingConfig.MEDIAN_BLUR_SIZE > 0) {
                Imgproc.medianBlur(maskMat, maskMat, ProcessingConfig.MEDIAN_BLUR_SIZE)
            }

            // 6. Morphological enhancement
            val kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, 
                Size(ProcessingConfig.MORPH_KERNEL_SIZE, ProcessingConfig.MORPH_KERNEL_SIZE)
            )
            
            // OPEN to remove tiny noise
            if (ProcessingConfig.MORPH_OPEN_ITERATIONS > 0) {
                Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_OPEN, kernel, Point(-1.0, -1.0), ProcessingConfig.MORPH_OPEN_ITERATIONS)
            }

            // ERODE to physically shrink blobs and break thin bridges
            if (ProcessingConfig.MORPH_ERODE_ITERATIONS > 0) {
                Imgproc.erode(maskMat, maskMat, kernel, Point(-1.0, -1.0), ProcessingConfig.MORPH_ERODE_ITERATIONS)
            }
            
            // CLOSE to fill tiny holes (only if enabled)
            if (ProcessingConfig.MORPH_CLOSE_ITERATIONS > 0) {
                val kernelClose = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT, 
                    Size(ProcessingConfig.MORPH_CLOSE_WIDTH, ProcessingConfig.MORPH_CLOSE_HEIGHT)
                )
                Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_CLOSE, kernelClose, Point(-1.0, -1.0), ProcessingConfig.MORPH_CLOSE_ITERATIONS)
                kernelClose.release()
            }
            
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
            diffMat.release()
        }
    }

    /**
     * Blacks out the left and right edges of the mask based on configuration.
     */
    private fun applyROIMask(mask: Mat) {
        val width = mask.cols()
        val height = mask.rows()
        
        val leftEdge = (width * ProcessingConfig.ROI_LEFT_PERCENT).toInt()
        val rightEdge = (width * (1.0 - ProcessingConfig.ROI_RIGHT_PERCENT)).toInt()
        
        // Black out left area
        if (leftEdge > 0) {
            val leftRoi = mask.submat(Rect(0, 0, leftEdge, height))
            leftRoi.setTo(Scalar(0.0))
            leftRoi.release()
        }
        
        // Black out right area
        if (rightEdge < width) {
            val rightRoi = mask.submat(Rect(rightEdge, 0, width - rightEdge, height))
            rightRoi.setTo(Scalar(0.0))
            rightRoi.release()
        }
    }

    /**
     * Detects fish blobs in the provided binary mask.
     */
    fun detectFish(mask: Mat): List<FishBlob> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        val tempBlobs = mutableListOf<FishBlob>()

        try {
            Imgproc.findContours(
                mask, 
                contours, 
                hierarchy, 
                Imgproc.RETR_EXTERNAL, 
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area >= ProcessingConfig.MIN_FISH_AREA && area <= ProcessingConfig.MAX_FISH_AREA) {
                    val rect = Imgproc.boundingRect(contour)
                    val moments = Imgproc.moments(contour)
                    if (moments._m00 != 0.0) {
                        val centerX = moments._m10 / moments._m00
                        val centerY = moments._m01 / moments._m00
                        
                        tempBlobs.add(
                            FishBlob(
                                center = Point(centerX, centerY),
                                boundingBox = rect,
                                area = area
                            )
                        )
                    }
                }
                contour.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fish detection failed", e)
        } finally {
            hierarchy.release()
        }

        return mergeBlobs(tempBlobs)
    }

    private fun mergeBlobs(blobs: List<FishBlob>): List<FishBlob> {
        if (blobs.size < 2 || ProcessingConfig.BLOB_MERGE_MAX_DISTANCE_Y <= 0) return blobs

        val merged = mutableListOf<FishBlob>()
        val used = BooleanArray(blobs.size)

        for (i in blobs.indices) {
            if (used[i]) continue
            
            var currentBlob = blobs[i]
            used[i] = true

            var foundMerge: Boolean
            do {
                foundMerge = false
                for (j in blobs.indices) {
                    if (used[j]) continue
                    
                    if (shouldMerge(currentBlob, blobs[j])) {
                        currentBlob = combineBlobs(currentBlob, blobs[j])
                        used[j] = true
                        foundMerge = true
                    }
                }
            } while (foundMerge)

            merged.add(currentBlob)
        }

        return merged
    }

    private fun shouldMerge(b1: FishBlob, b2: FishBlob): Boolean {
        val rect1 = b1.boundingBox
        val rect2 = b2.boundingBox

        val verticalDist = if (rect1.y < rect2.y) {
            rect2.y - (rect1.y + rect1.height)
        } else {
            rect1.y - (rect2.y + rect2.height)
        }

        val overlapX = Math.min(rect1.x + rect1.width, rect2.x + rect2.width) - Math.max(rect1.x, rect2.x)

        return verticalDist <= ProcessingConfig.BLOB_MERGE_MAX_DISTANCE_Y && 
               overlapX >= ProcessingConfig.BLOB_MERGE_MIN_OVERLAP_X
    }

    private fun combineBlobs(b1: FishBlob, b2: FishBlob): FishBlob {
        val r1 = b1.boundingBox
        val r2 = b2.boundingBox

        val x = Math.min(r1.x, r2.x)
        val y = Math.min(r1.y, r2.y)
        val width = Math.max(r1.x + r1.width, r2.x + r2.width) - x
        val height = Math.max(r1.y + r1.height, r2.y + r2.height) - y
        
        val mergedRect = Rect(x, y, width, height)
        val mergedArea = b1.area + b2.area
        
        val centerX = x + width / 2.0
        val centerY = y + height / 2.0

        return FishBlob(
            center = Point(centerX, centerY),
            boundingBox = mergedRect,
            area = mergedArea
        )
    }

    fun drawDetections(frame: Mat, blobs: List<FishBlob>) {
        val color = Scalar(0.0, 255.0, 0.0) // Green
        val thickness = 2

        for (blob in blobs) {
            Imgproc.rectangle(frame, blob.boundingBox.tl(), blob.boundingBox.br(), color, thickness)
            Imgproc.circle(frame, blob.center, 4, color, -1)
            
            if (blob.id != -1) {
                Imgproc.putText(
                    frame,
                    "ID: ${blob.id} (${if (blob.isCounted) "C" else "UC"})",
                    Point(blob.boundingBox.x.toDouble(), (blob.boundingBox.y - 10).toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    color,
                    1
                )
            }
        }
    }

    fun drawCountingLine(frame: Mat) {
        val lineY = (frame.rows() * ProcessingConfig.COUNTING_LINE_Y_PERCENT).toInt()
        val startPoint = Point(0.0, lineY.toDouble())
        val endPoint = Point(frame.cols().toDouble(), lineY.toDouble())
        
        Imgproc.line(frame, startPoint, endPoint, Scalar(255.0, 0.0, 0.0), 2)
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
            if (mat.empty()) return null
            val bitmap = if (mat.channels() == 1) {
                val bgraMat = Mat()
                Imgproc.cvtColor(mat, bgraMat, Imgproc.COLOR_GRAY2BGRA)
                val resultBitmap = createBitmap(bgraMat.cols(), bgraMat.rows(), Bitmap.Config.ARGB_8888)
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

    fun convertToGrayscale(colorMat: Mat): Mat {
        if (colorMat.channels() == 1) return colorMat.clone()
        val grayMat = Mat()
        when (colorMat.channels()) {
            4 -> Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
            3 -> Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)
            else -> return colorMat.clone()
        }
        return grayMat
    }
}
