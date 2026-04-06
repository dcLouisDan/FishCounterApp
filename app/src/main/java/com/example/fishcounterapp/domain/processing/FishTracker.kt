package com.example.fishcounterapp.domain.processing

import com.example.fishcounterapp.utils.ProcessingConfig
import org.opencv.core.Point
import org.opencv.core.Rect
import kotlin.math.sqrt

/**
 * Centroid Tracker for maintaining identities of detected fish across frames.
 */
class FishTracker {
    private var nextId = 0
    private var trackedBlobs = mutableListOf<FishBlob>()

    /**
     * Updates the tracker with new detections and detects line crossings.
     */
    fun update(
        newDetections: List<FishBlob>, 
        lineY: Int,
        onFishCrossed: () -> Unit
    ): List<FishBlob> {
        if (newDetections.isEmpty()) {
            trackedBlobs = trackedBlobs.map { it.copy(framesLost = it.framesLost + 1) }
                .filter { it.framesLost <= ProcessingConfig.TRACKING_MAX_LOST_FRAMES }
                .toMutableList()
            return emptyList()
        }

        if (trackedBlobs.isEmpty()) {
            for (detection in newDetections) {
                registerBlob(detection, lineY)
            }
            return trackedBlobs.toList()
        }

        val updatedTrackedBlobs = mutableListOf<FishBlob>()
        val usedDetectionIndices = mutableSetOf<Int>()

        // 1. Match existing blobs
        for (i in trackedBlobs.indices) {
            val tracked = trackedBlobs[i]
            var bestMatchIndex = -1
            var minDistance = Double.MAX_VALUE
            var maxOverlap = 0.0

            for (j in newDetections.indices) {
                if (j in usedDetectionIndices) continue
                
                val detection = newDetections[j]
                val overlap = calculateOverlapArea(tracked.boundingBox, detection.boundingBox)
                val dist = distance(tracked.center, detection.center)

                if (overlap > 0 && overlap > maxOverlap) {
                    maxOverlap = overlap
                    bestMatchIndex = j
                } else if (maxOverlap == 0.0 && dist < minDistance && dist <= ProcessingConfig.TRACKING_MAX_DISTANCE) {
                    minDistance = dist
                    bestMatchIndex = j
                }
            }

            if (bestMatchIndex != -1) {
                val newDetection = newDetections[bestMatchIndex]
                val newCenter = newDetection.center
                
                // --- Robust State-Based Counting ---
                val isCurrentlyAbove = newCenter.y < lineY
                val isPastBand = newCenter.y > (lineY + ProcessingConfig.COUNTING_BAND_HEIGHT)
                
                var fishIsCounted = tracked.isCounted
                var fishCanBeCounted = tracked.canBeCounted

                // If not eligible yet, check if it's well above the line
                if (!fishIsCounted && !fishCanBeCounted) {
                    if (newCenter.y < (lineY - 10)) {
                        fishCanBeCounted = true
                    }
                }

                // If eligible and has crossed the band, count it
                if (fishCanBeCounted && !fishIsCounted && isPastBand) {
                    if (tracked.consecutiveFramesSeen >= ProcessingConfig.TRACKING_MIN_STABILITY_FRAMES) {
                        onFishCrossed()
                        fishIsCounted = true
                        fishCanBeCounted = false // Reset eligibility after counting
                    }
                }

                updatedTrackedBlobs.add(
                    newDetection.copy(
                        id = tracked.id,
                        framesLost = 0,
                        wasAboveLine = isCurrentlyAbove,
                        isCounted = fishIsCounted,
                        canBeCounted = fishCanBeCounted,
                        consecutiveFramesSeen = tracked.consecutiveFramesSeen + 1,
                        initialY = tracked.initialY
                    )
                )
                usedDetectionIndices.add(bestMatchIndex)
            } else {
                val lostBlob = tracked.copy(framesLost = tracked.framesLost + 1)
                if (lostBlob.framesLost <= ProcessingConfig.TRACKING_MAX_LOST_FRAMES) {
                    updatedTrackedBlobs.add(lostBlob)
                }
            }
        }

        // 2. Register remaining new detections
        for (j in newDetections.indices) {
            if (j !in usedDetectionIndices) {
                val newCenter = newDetections[j].center
                // Only track fish that appear near or above the line, ignore those spawning way below
                if (newCenter.y < (lineY + ProcessingConfig.COUNTING_BAND_HEIGHT)) {
                    val isAbove = newCenter.y < lineY
                    updatedTrackedBlobs.add(
                        newDetections[j].copy(
                            id = nextId++, 
                            framesLost = 0,
                            wasAboveLine = isAbove,
                            canBeCounted = isAbove, // Eligible if starts above
                            consecutiveFramesSeen = 1,
                            initialY = newCenter.y
                        )
                    )
                }
            }
        }

        trackedBlobs = updatedTrackedBlobs
        return trackedBlobs.filter { it.framesLost == 0 }
    }

    private fun registerBlob(detection: FishBlob, lineY: Int) {
        val isAbove = detection.center.y < lineY
        trackedBlobs.add(
            detection.copy(
                id = nextId++, 
                framesLost = 0,
                wasAboveLine = isAbove,
                canBeCounted = isAbove,
                consecutiveFramesSeen = 1,
                initialY = detection.center.y
            )
        )
    }

    private fun distance(p1: Point, p2: Point): Double {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateOverlapArea(r1: Rect, r2: Rect): Double {
        val xOverlap = Math.max(0, Math.min(r1.x + r1.width, r2.x + r2.width) - Math.max(r1.x, r2.x))
        val yOverlap = Math.max(0, Math.min(r1.y + r1.height, r2.y + r2.height) - Math.max(r1.y, r2.y))
        return (xOverlap * yOverlap).toDouble()
    }

    fun reset() {
        nextId = 0
        trackedBlobs.clear()
    }
}
