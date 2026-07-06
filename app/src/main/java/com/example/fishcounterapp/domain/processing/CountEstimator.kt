package com.example.fishcounterapp.domain.processing

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class CountEstimatorConfig(
    val averageFishArea: Double,
    val expectedTubeWidth: Double,
    val minimumAreaRatio: Double,
    val singleFishAreaTolerance: Double,
    val widthFishRatio: Double,
    val maxFishPerBlob: Int
)

data class EstimatedCount(
    val count: Int,
    val confidence: CountConfidence
)

enum class CountConfidence {
    HIGH,
    MEDIUM,
    LOW
}

class CountEstimator(
    private val config: CountEstimatorConfig
) {

    fun estimate(blob: FishBlob): EstimatedCount {
        val areaRatio = blob.area / config.averageFishArea
        if (areaRatio < config.minimumAreaRatio) {
            return EstimatedCount(count = 0, confidence = CountConfidence.LOW)
        }

        val areaCount = areaRatio.roundToInt().coerceAtLeast(1)
        val widthCount = floor(blob.boundingBox.width / (config.expectedTubeWidth * config.widthFishRatio))
            .toInt()
            .coerceAtLeast(1)
        val rawCount = max(areaCount, widthCount)
        val count = rawCount.coerceAtMost(config.maxFishPerBlob)

        return EstimatedCount(
            count = count,
            confidence = confidenceFor(areaRatio, rawCount, count)
        )
    }

    private fun confidenceFor(areaRatio: Double, rawCount: Int, count: Int): CountConfidence {
        if (rawCount > count) return CountConfidence.LOW

        return when {
            count == 1 && areaRatio <= 1.0 + config.singleFishAreaTolerance -> CountConfidence.HIGH
            count == 1 -> CountConfidence.MEDIUM
            else -> CountConfidence.MEDIUM
        }
    }
}
