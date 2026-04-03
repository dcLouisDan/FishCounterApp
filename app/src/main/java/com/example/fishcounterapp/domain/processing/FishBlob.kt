package com.example.fishcounterapp.domain.processing

import org.opencv.core.Point
import org.opencv.core.Rect

/**
 * Represents a detected object (fish) in a frame.
 * 
 * @property id Unique identifier for tracking across frames.
 * @property center The centroid of the detected blob.
 * @property boundingBox The rectangular area enclosing the blob.
 * @property area The total area (in pixels) of the blob.
 * @property framesLost Number of consecutive frames this blob has been missing.
 * @property wasAboveLine Tracks if the fish was above the counting line in the previous frame.
 * @property isCounted Whether this fish has already been counted.
 * @property consecutiveFramesSeen How many consecutive frames this fish has been tracked.
 * @property initialY The Y coordinate where this fish was first detected.
 */
data class FishBlob(
    val id: Int = -1,
    val center: Point,
    val boundingBox: Rect,
    val area: Double,
    val framesLost: Int = 0,
    val wasAboveLine: Boolean? = null,
    val isCounted: Boolean = false,
    val consecutiveFramesSeen: Int = 1,
    val initialY: Double = center.y
)
