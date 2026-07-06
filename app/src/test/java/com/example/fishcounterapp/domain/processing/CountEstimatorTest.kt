package com.example.fishcounterapp.domain.processing

import org.junit.Assert.assertEquals
import org.junit.Test
import org.opencv.core.Point
import org.opencv.core.Rect

class CountEstimatorTest {

    @Test
    fun estimatesOneHighConfidenceFishForSingleFishSizedBlob() {
        val estimate = estimator().estimate(blob(area = 100.0, width = 18, height = 8))

        assertEquals(1, estimate.count)
        assertEquals(CountConfidence.HIGH, estimate.confidence)
    }

    @Test
    fun ignoresTinyNoiseBelowMinimumAreaRatio() {
        val estimate = estimator().estimate(blob(area = 24.0, width = 8, height = 4))

        assertEquals(0, estimate.count)
        assertEquals(CountConfidence.LOW, estimate.confidence)
    }

    @Test
    fun estimatesMergedBlobCountFromArea() {
        val estimate = estimator().estimate(blob(area = 255.0, width = 26, height = 12))

        assertEquals(3, estimate.count)
        assertEquals(CountConfidence.MEDIUM, estimate.confidence)
    }

    @Test
    fun estimatesMergedBlobCountFromTubeWidthOccupancy() {
        val estimate = estimator().estimate(blob(area = 150.0, width = 46, height = 10))

        assertEquals(2, estimate.count)
        assertEquals(CountConfidence.MEDIUM, estimate.confidence)
    }

    @Test
    fun treatsModerateSizeVariationAsOneFish() {
        val estimate = estimator().estimate(blob(area = 145.0, width = 23, height = 9))

        assertEquals(1, estimate.count)
        assertEquals(CountConfidence.HIGH, estimate.confidence)
    }

    @Test
    fun clampsLargeMergedBlobToMaximumConfiguredCount() {
        val estimate = estimator().estimate(blob(area = 900.0, width = 120, height = 20))

        assertEquals(4, estimate.count)
        assertEquals(CountConfidence.LOW, estimate.confidence)
    }

    private fun estimator(): CountEstimator {
        return CountEstimator(
            CountEstimatorConfig(
                averageFishArea = 100.0,
                expectedTubeWidth = 24.0,
                minimumAreaRatio = 0.35,
                singleFishAreaTolerance = 0.55,
                widthFishRatio = 0.82,
                maxFishPerBlob = 4
            )
        )
    }

    private fun blob(area: Double, width: Int, height: Int): FishBlob {
        return FishBlob(
            center = Point(width / 2.0, height / 2.0),
            boundingBox = Rect(0, 0, width, height),
            area = area
        )
    }
}
