package com.example.fishcounterapp.domain.processing

import org.opencv.core.Point
import org.opencv.core.Rect

/**
 * Represents a detected object (fish) in a frame.
 * 
 * @property center The centroid of the detected blob.
 * @property boundingBox The rectangular area enclosing the blob.
 * @property area The total area (in pixels) of the blob.
 */
data class FishBlob(
    val center: Point,
    val boundingBox: Rect,
    val area: Double
)
