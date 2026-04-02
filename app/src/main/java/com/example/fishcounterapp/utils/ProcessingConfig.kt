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
     * Threshold for background subtraction.
     * Higher values = less sensitive to noise/subtle light changes.
     * Lower values = more sensitive (detects smaller/fainter movements).
     */
    const val SUBTRACTION_THRESHOLD = 35.0
    const val BINARY_MAX_VALUE = 255.0

    // Performance monitoring
    const val FPS_UPDATE_INTERVAL_MS = 1000L  // Update FPS every second

    // Logging
    const val ENABLE_VERBOSE_LOGGING = true  // Set false for production
    const val LOG_FRAME_TIMING = true        // Log individual frame times

    // Memory management
    const val MAT_CACHE_SIZE = 3  // Number of Mats to keep in pool (future optimization)
}