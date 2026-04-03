package com.example.fishcounterapp.utils

/**
 * Central configuration for image processing parameters
 */
object ProcessingConfig {

    // JPEG quality for fallback bitmap conversion (0-100)
    const val JPEG_QUALITY = 100

    // Target camera resolution (lower = faster processing)
    const val TARGET_WIDTH = 640
    const val TARGET_HEIGHT = 480

    // Grayscale conversion
    const val GRAYSCALE_ENABLED_BY_DEFAULT = false

    // Background Subtraction & Thresholding
    /** 
     * Increased to 50.0 for extreme stability against simulator wobble.
     */
    const val SUBTRACTION_THRESHOLD = 50.0
    const val BINARY_MAX_VALUE = 255.0

    // Noise Reduction Parameters
    const val GAUSSIAN_BLUR_SIZE = 7.0 // Blur before subtraction
    const val MEDIAN_BLUR_SIZE = 5     // Blur the binary mask (must be odd)

    // Morphological Operations
    const val MORPH_KERNEL_SIZE = 3.0
    const val MORPH_OPEN_ITERATIONS = 1
    const val MORPH_DILATE_ITERATIONS = 1 // Minimized to prevent merging

    // Blob Detection (Contour Filtering)
    const val MIN_FISH_AREA = 120.0
    const val MAX_FISH_AREA = 10000.0

    // Counting Line Configuration
    const val COUNTING_LINE_Y_PERCENT = 0.5

    // Tracking Configuration
    const val TRACKING_MAX_DISTANCE = 70.0
    const val TRACKING_MAX_LOST_FRAMES = 5

    // Performance monitoring
    const val FPS_UPDATE_INTERVAL_MS = 1000L

    // Logging
    const val ENABLE_VERBOSE_LOGGING = true
    const val LOG_FRAME_TIMING = true

    // Memory management
    const val MAT_CACHE_SIZE = 3
}