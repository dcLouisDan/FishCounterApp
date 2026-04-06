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
    /**
     * Increased to 65.0 for maximum selectivity and separation.
     */
    const val SUBTRACTION_THRESHOLD = 65.0 
    const val BINARY_MAX_VALUE = 255.0

    // Noise Reduction - Maximum Sharpness
    /**
     * Set to 0.0 to disable pre-subtraction blurring. 
     * This keeps the fish edges at their absolute sharpest.
     */
    const val GAUSSIAN_BLUR_SIZE = 0.0 
    
    /**
     * Minimal median filter to preserve the smallest gaps.
     */
    const val MEDIAN_BLUR_SIZE = 3     

    // Morphology - Focused on Separation
    const val MORPH_KERNEL_SIZE = 3.0
    
    /**
     * Disabled closing.
     */
    const val MORPH_CLOSE_WIDTH = 3.0
    const val MORPH_CLOSE_HEIGHT = 3.0 
    const val MORPH_CLOSE_ITERATIONS = 0
    
    /**
     * Minimal open to remove noise.
     */
    const val MORPH_OPEN_ITERATIONS = 1 
    
    /**
     * Increased to 2 iterations to aggressively shrink blobs 
     * and physically "cut" any thin bridges between fish.
     */
    const val MORPH_ERODE_ITERATIONS = 2
    const val MORPH_DILATE_ITERATIONS = 0 

    // Region of Interest (ROI)
    const val ROI_LEFT_PERCENT = 0.18 
    const val ROI_RIGHT_PERCENT = 0.18

    // Blob Detection & Merging
    /**
     * Lowered to 80.0 because 2 iterations of Erosion will 
     * significantly shrink the fish blobs.
     */
    const val MIN_FISH_AREA = 80.0
    const val MAX_FISH_AREA = 30000.0

    const val BLOB_MERGE_MAX_DISTANCE_Y = 0.0
    const val BLOB_MERGE_MIN_OVERLAP_X = 5.0

    // Counting Line Configuration
    const val COUNTING_LINE_Y_PERCENT = 0.5
    const val COUNTING_BAND_HEIGHT = 8.0 

    // Tracking Configuration
    const val TRACKING_MAX_DISTANCE = 70.0
    const val TRACKING_MAX_LOST_FRAMES = 1 
    const val TRACKING_MIN_STABILITY_FRAMES = 1

    // Performance monitoring
    const val FPS_UPDATE_INTERVAL_MS = 1000L

    // Logging
    const val ENABLE_VERBOSE_LOGGING = true
    const val LOG_FRAME_TIMING = true

    // Memory management
    const val MAT_CACHE_SIZE = 3
}
