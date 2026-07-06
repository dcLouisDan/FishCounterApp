package com.example.fishcounterapp.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class Yuv420Nv21PackerTest {

    @Test
    fun packsContiguousYuv420PlanesIntoNv21() {
        val y = byteBufferOf(
            1, 2, 3, 4,
            5, 6, 7, 8
        )
        val u = byteBufferOf(21, 22)
        val v = byteBufferOf(31, 32)

        val nv21 = Yuv420Nv21Packer.pack(
            width = 4,
            height = 2,
            yPlane = plane(y, rowStride = 4, pixelStride = 1),
            uPlane = plane(u, rowStride = 2, pixelStride = 1),
            vPlane = plane(v, rowStride = 2, pixelStride = 1)
        )

        assertArrayEquals(
            bytesOf(1, 2, 3, 4, 5, 6, 7, 8, 31, 21, 32, 22),
            nv21
        )
    }

    @Test
    fun ignoresRowPaddingWhenPackingLumaAndChroma() {
        val y = byteBufferOf(
            1, 2, 3, 99, 99,
            4, 5, 6, 99, 99
        )
        val u = byteBufferOf(21, 22, 99, 99)
        val v = byteBufferOf(31, 32, 99, 99)

        val nv21 = Yuv420Nv21Packer.pack(
            width = 3,
            height = 2,
            yPlane = plane(y, rowStride = 5, pixelStride = 1),
            uPlane = plane(u, rowStride = 4, pixelStride = 1),
            vPlane = plane(v, rowStride = 4, pixelStride = 1)
        )

        assertArrayEquals(
            bytesOf(1, 2, 3, 4, 5, 6, 31, 21, 32, 22),
            nv21
        )
    }

    @Test
    fun packsPixelStrideTwoChromaAsVisibleVuSamples() {
        val y = byteBufferOf(
            1, 2, 3, 4,
            5, 6, 7, 8
        )
        val u = byteBufferOf(21, 91, 22, 92)
        val v = byteBufferOf(31, 81, 32, 82)

        val nv21 = Yuv420Nv21Packer.pack(
            width = 4,
            height = 2,
            yPlane = plane(y, rowStride = 4, pixelStride = 1),
            uPlane = plane(u, rowStride = 4, pixelStride = 2),
            vPlane = plane(v, rowStride = 4, pixelStride = 2)
        )

        assertArrayEquals(
            bytesOf(1, 2, 3, 4, 5, 6, 7, 8, 31, 21, 32, 22),
            nv21
        )
    }

    @Test
    fun preservesSourceBufferPositions() {
        val y = byteBufferOf(1, 2, 3, 4)
        val u = byteBufferOf(21)
        val v = byteBufferOf(31)
        y.position(1)
        u.position(1)
        v.position(1)

        Yuv420Nv21Packer.pack(
            width = 2,
            height = 2,
            yPlane = plane(y, rowStride = 2, pixelStride = 1),
            uPlane = plane(u, rowStride = 1, pixelStride = 1),
            vPlane = plane(v, rowStride = 1, pixelStride = 1)
        )

        assertEquals(1, y.position())
        assertEquals(1, u.position())
        assertEquals(1, v.position())
    }

    private fun plane(
        buffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int
    ): Yuv420Nv21Packer.Plane {
        return Yuv420Nv21Packer.Plane(
            buffer = buffer,
            rowStride = rowStride,
            pixelStride = pixelStride
        )
    }

    private fun byteBufferOf(vararg values: Int): ByteBuffer {
        return ByteBuffer.wrap(bytesOf(*values))
    }

    private fun bytesOf(vararg values: Int): ByteArray {
        return ByteArray(values.size) { index -> values[index].toByte() }
    }
}
