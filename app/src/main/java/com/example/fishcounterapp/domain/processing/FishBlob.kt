package com.example.fishcounterapp.domain.processing

import org.opencv.core.Point
import org.opencv.core.Rect

/**
 * Represents a detected object (fish) in a frame.
 */
data class FishBlob(
    val id: Int = -1,
    val center: Point,
    val boundingBox: Rect,
    val area: Double,
    val framesLost: Int = 0,
    val wasAboveLine: Boolean? = null,
    val isCounted: Boolean = false,
    val canBeCounted: Boolean = false, // Eligibility for counting
    val consecutiveFramesSeen: Int = 1,
    val initialY: Double = center.y
)
