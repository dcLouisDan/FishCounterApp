package com.example.fishcounterapp.utils

import java.nio.ByteBuffer

/**
 * Packs Android YUV_420_888 planes into NV21 byte layout.
 */
object Yuv420Nv21Packer {

    data class Plane(
        val buffer: ByteBuffer,
        val rowStride: Int,
        val pixelStride: Int
    )

    fun pack(
        width: Int,
        height: Int,
        yPlane: Plane,
        uPlane: Plane,
        vPlane: Plane
    ): ByteArray {
        val ySize = width * height
        val chromaWidth = (width + 1) / 2
        val chromaHeight = (height + 1) / 2
        val output = ByteArray(ySize + chromaWidth * chromaHeight * 2)

        var outputIndex = 0
        for (row in 0 until height) {
            val rowStart = row * yPlane.rowStride
            for (col in 0 until width) {
                output[outputIndex++] = yPlane.buffer.get(rowStart + col * yPlane.pixelStride)
            }
        }

        for (row in 0 until chromaHeight) {
            val uRowStart = row * uPlane.rowStride
            val vRowStart = row * vPlane.rowStride
            for (col in 0 until chromaWidth) {
                output[outputIndex++] = vPlane.buffer.get(vRowStart + col * vPlane.pixelStride)
                output[outputIndex++] = uPlane.buffer.get(uRowStart + col * uPlane.pixelStride)
            }
        }

        return output
    }
}
