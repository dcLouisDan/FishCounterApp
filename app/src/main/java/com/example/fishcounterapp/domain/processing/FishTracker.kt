package com.example.fishcounterapp.domain.processing

import com.example.fishcounterapp.utils.ProcessingConfig
import org.opencv.core.Point
import kotlin.math.sqrt

/**
 * Centroid Tracker for maintaining identities of detected fish across frames.
 */
class FishTracker {
    private var nextId = 0
    private var trackedBlobs = mutableListOf<FishBlob>()

    /**
     * Updates the tracker with new detections and detects line crossings.
     * 
     * @param newDetections List of blobs detected in the current frame.
     * @param lineY The vertical position of the counting line in pixels.
     * @param onFishCrossed Callback triggered when a fish crosses the line.
     * @return List of tracked blobs seen in this frame.
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
            var minDistance = Double.MAX_VALUE
            var bestMatchIndex = -1

            for (j in newDetections.indices) {
                if (j in usedDetectionIndices) continue
                
                val dist = distance(tracked.center, newDetections[j].center)
                if (dist < minDistance && dist <= ProcessingConfig.TRACKING_MAX_DISTANCE) {
                    minDistance = dist
                    bestMatchIndex = j
                }
            }

            if (bestMatchIndex != -1) {
                val newCenter = newDetections[bestMatchIndex].center
                val isCurrentlyAbove = newCenter.y < lineY

                // Check for Crossing: Was above, now is on/below
                if (tracked.wasAboveLine == true && !isCurrentlyAbove) {
                    onFishCrossed()
                }

                updatedTrackedBlobs.add(
                    newDetections[bestMatchIndex].copy(
                        id = tracked.id,
                        framesLost = 0,
                        wasAboveLine = isCurrentlyAbove
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

        // 2. Register new detections
        for (j in newDetections.indices) {
            if (j !in usedDetectionIndices) {
                val newCenter = newDetections[j].center
                val isAbove = newCenter.y < lineY
                
                updatedTrackedBlobs.add(
                    newDetections[j].copy(
                        id = nextId++, 
                        framesLost = 0,
                        wasAboveLine = isAbove
                    )
                )
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
                wasAboveLine = isAbove
            )
        )
    }

    private fun distance(p1: Point, p2: Point): Double {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    fun reset() {
        nextId = 0
        trackedBlobs.clear()
    }
}
