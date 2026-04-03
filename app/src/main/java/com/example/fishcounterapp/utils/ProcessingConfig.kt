package com.example.fishcounterapp.utils

/**
 * Central configuration for image processing parameters
 */
object ProcessingConfig {

    // JPEG quality for fallback bitmap conversion (0-100)
    const val JPEG_QUALITY = 100

    // Target camera resolution
    const val TARGET_WIDTH = 640
    const val TARGET_HEIGHT = 480

    const val GRAYSCALE_ENABLED_BY_DEFAULT = false

    // Background Subtraction
    const val SUBTRACTION_THRESHOLD = 40.0
    const val BINARY_MAX_VALUE = 255.0

    // Noise Reduction
    const val GAUSSIAN_BLUR_SIZE = 7.0
    const val MEDIAN_BLUR_SIZE = 5

    // Morphology - Vertically biased for fish flow
    const val MORPH_KERNEL_SIZE = 3.0
    const val MORPH_CLOSE_WIDTH = 3.0
    const val MORPH_CLOSE_HEIGHT = 10.0
    const val MORPH_CLOSE_ITERATIONS = 2
    const val MORPH_OPEN_ITERATIONS = 1
    const val MORPH_DILATE_ITERATIONS = 1

    // Blob Detection & Merging
    const val MIN_FISH_AREA = 100.0
    const val MAX_FISH_AREA = 30000.0

    /**
     * Increased from 40 to 60 to better handle fragmentation of larger fish.
     */
    const val BLOB_MERGE_MAX_DISTANCE_Y = 30.0
    const val BLOB_MERGE_MIN_OVERLAP_X = 5.0

    // Counting Line Configuration
    const val COUNTING_LINE_Y_PERCENT = 0.5

    // Tracking Configuration
    /**
     * Increased from 80 to 120. 
     * This helps track larger objects that "jump" more during wobbles.
     */
    const val TRACKING_MAX_DISTANCE = 120.0
    const val TRACKING_MAX_LOST_FRAMES = 8

    // Performance monitoring
    const val FPS_UPDATE_INTERVAL_MS = 1000L

    // Logging
    const val ENABLE_VERBOSE_LOGGING = true
    const val LOG_FRAME_TIMING = true

    // Memory management
    const val MAT_CACHE_SIZE = 3
}