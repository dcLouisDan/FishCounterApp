package com.example.fishcounterapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

/**
 * Utility class for converting between different image formats used in the app.
 *
 * Conversion paths supported:
 * - ImageProxy (YUV_420_888) → Mat (BGR) [Optimized, no Bitmap intermediate]
 * - ImageProxy (YUV_420_888) → Bitmap (ARGB) [Fallback method via JPEG]
 * - Bitmap (ARGB) → Mat (BGR)
 *
 * Performance characteristics:
 * - Direct YUV→Mat: ~8-12ms for 640x480 resolution
 * - Fallback Bitmap method: ~26ms for 640x480 resolution
 *
 * @see ProcessingConfig for tunable parameters
 */
object ImageConverter {

    private const val TAG = "ImageConverter"
    
    // Performance tracking
    private var directConversionAttempts = 0
    private var directConversionFailures = 0
    private var fallbackConversionAttempts = 0

    data class ConversionStats(
        val directAttempts: Int,
        val directFailures: Int,
        val fallbackAttempts: Int,
        val directSuccessRate: Double
    )

    /**
     * Get conversion statistics for monitoring
     */
    fun getStats(): ConversionStats {
        return ConversionStats(
            directAttempts = directConversionAttempts,
            directFailures = directConversionFailures,
            fallbackAttempts = fallbackConversionAttempts,
            directSuccessRate = if (directConversionAttempts > 0) {
                ((directConversionAttempts - directConversionFailures) * 100.0 / directConversionAttempts)
            } else 0.0
        )
    }
    
    /**
     * Converts ImageProxy directly to OpenCV Mat.
     * 
     * This is the optimized path that bypasses Bitmap creation entirely.
     * Handles YUV_420_888 format with various pixel/row stride configurations.
     * 
     * Performance: ~8-12ms for 640x480 on typical devices
     * 
     * @param imageProxy Camera frame in YUV_420_888 format
     * @return Mat in BGR format (suitable for OpenCV operations), or null on failure
     */
    fun imageProxyToMatDirect(imageProxy: ImageProxy): Mat? {
        directConversionAttempts++
        return try {
            if (imageProxy.format != ImageFormat.YUV_420_888) {
                Log.e(TAG, "Unsupported format: ${imageProxy.format}")
                directConversionFailures++
                return null
            }

            val width = imageProxy.width
            val height = imageProxy.height

            // 1. Get the planes
            val yPlane = imageProxy.planes[0]
            val uPlane = imageProxy.planes[1]
            val vPlane = imageProxy.planes[2]

            val nv21 = Yuv420Nv21Packer.pack(
                width = width,
                height = height,
                yPlane = yPlane.toPackerPlane(),
                uPlane = uPlane.toPackerPlane(),
                vPlane = vPlane.toPackerPlane()
            )

            // 4. Create YUV Mat (Height * 1.5 to account for Chroma planes)
            val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
            yuvMat.put(0, 0, nv21)

            // 5. Convert to BGR or RGBA
            val bgrMat = Mat()
            Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2RGBA_NV21)
            yuvMat.release()

            // 6. Handle Rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val rotatedMat = rotateMat(bgrMat, rotationDegrees)
                bgrMat.release()
                rotatedMat
            } else {
                bgrMat
            }
        } catch (e: Exception) {
            directConversionFailures++
            Log.e(TAG, "Direct conversion failed ($directConversionFailures/$directConversionAttempts)", e)
            
            // Fallback
            fallbackConversionAttempts++
            imageProxyToBitmapFallback(imageProxy)?.let { bitmapToMat(it) }
        }
    }

    private fun rotateMat(mat: Mat, degrees: Int): Mat {
        if (degrees == 0) return mat

        val rotatedMat = Mat()

        when (degrees) {
            90 -> {
                org.opencv.core.Core.rotate(
                    mat,
                    rotatedMat,
                    org.opencv.core.Core.ROTATE_90_CLOCKWISE
                )
            }

            180 -> {
                org.opencv.core.Core.rotate(mat, rotatedMat, org.opencv.core.Core.ROTATE_180)
            }

            270 -> {
                org.opencv.core.Core.rotate(
                    mat,
                    rotatedMat,
                    org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE
                )
            }

            else -> {
                Log.w(TAG, "Unsupported rotation: $degrees degrees")
                return mat
            }
        }

        return rotatedMat
    }

    /**
     * Fallback method: ImageProxy → Bitmap (via JPEG)
     * Used when direct conversion fails or for comparison
     */
    fun imageProxyToBitmapFallback(imageProxy: ImageProxy): Bitmap? {
        return try {
            val nv21 = Yuv420Nv21Packer.pack(
                width = imageProxy.width,
                height = imageProxy.height,
                yPlane = imageProxy.planes[0].toPackerPlane(),
                uPlane = imageProxy.planes[1].toPackerPlane(),
                vPlane = imageProxy.planes[2].toPackerPlane()
            )

            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, imageProxy.width, imageProxy.height),
                ProcessingConfig.JPEG_QUALITY,
                out
            )

            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Handle rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                rotateBitmap(bitmap, rotationDegrees)
            } else {
                bitmap
            }

        } catch (e: Exception) {
            Log.e(TAG, "Bitmap fallback conversion failed", e)
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Convert Bitmap to Mat (for fallback path or when Bitmap is needed)
     */
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    private fun ImageProxy.PlaneProxy.toPackerPlane(): Yuv420Nv21Packer.Plane {
        return Yuv420Nv21Packer.Plane(
            buffer = buffer,
            rowStride = rowStride,
            pixelStride = pixelStride
        )
    }
}
