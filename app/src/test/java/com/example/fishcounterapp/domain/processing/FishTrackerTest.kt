package com.example.fishcounterapp.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.opencv.core.Point
import org.opencv.core.Rect

class FishTrackerTest {

    @Test
    fun countsFishOnceWhenItCrossesCountingBandFromAbove() {
        val tracker = FishTracker()
        var count = 0

        tracker.update(listOf(blobAt(y = 40.0)), LINE_Y) { count++ }
        tracker.update(listOf(blobAt(y = 85.0)), LINE_Y) { count++ }
        tracker.update(listOf(blobAt(y = 112.0)), LINE_Y) { count++ }
        tracker.update(listOf(blobAt(y = 125.0)), LINE_Y) { count++ }

        assertEquals(1, count)
    }

    @Test
    fun doesNotCountFishThatFirstAppearsBelowTheCountingLine() {
        val tracker = FishTracker()
        var count = 0

        tracker.update(listOf(blobAt(y = 112.0)), LINE_Y) { count++ }
        tracker.update(listOf(blobAt(y = 125.0)), LINE_Y) { count++ }
        tracker.update(listOf(blobAt(y = 140.0)), LINE_Y) { count++ }

        assertEquals(0, count)
    }

    @Test
    fun preservesTrackingIdAcrossSmallMovements() {
        val tracker = FishTracker()

        val firstFrame = tracker.update(listOf(blobAt(x = 120.0, y = 40.0)), LINE_Y) {}
        val secondFrame = tracker.update(listOf(blobAt(x = 126.0, y = 48.0)), LINE_Y) {}

        assertEquals(1, firstFrame.size)
        assertEquals(1, secondFrame.size)
        assertEquals(firstFrame.single().id, secondFrame.single().id)
    }

    @Test
    fun removesLostTrackAfterConfiguredLostFrameLimit() {
        val tracker = FishTracker()

        val initialTrack = tracker.update(listOf(blobAt(y = 40.0)), LINE_Y) {}
        tracker.update(emptyList(), LINE_Y) {}
        val visibleTracksAfterLimit = tracker.update(emptyList(), LINE_Y) {}
        val newTrack = tracker.update(listOf(blobAt(y = 42.0)), LINE_Y) {}

        assertEquals(emptyList<FishBlob>(), visibleTracksAfterLimit)
        assertNotEquals(initialTrack.single().id, newTrack.single().id)
    }

    private fun blobAt(
        x: Double = 120.0,
        y: Double,
        width: Int = 20,
        height: Int = 20
    ): FishBlob {
        val left = (x - width / 2.0).toInt()
        val top = (y - height / 2.0).toInt()

        return FishBlob(
            center = Point(x, y),
            boundingBox = Rect(left, top, width, height),
            area = (width * height).toDouble()
        )
    }

    private companion object {
        const val LINE_Y = 100
    }
}
