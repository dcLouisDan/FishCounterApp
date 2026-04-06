# Fish Counter Mobile App - Project Documentation

**Last Updated:** October 26, 2024  
**Current Status:** Phase 3 COMPLETED (100% Accuracy in Simulator)  
**Developer:** Dan (BS IT, Junior Software Developer, Pampanga, Philippines)

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Technology Stack](#technology-stack)
4. [Development Progress](#development-progress)
5. [Performance Benchmarks](#performance-benchmarks)
6. [Code Structure](#code-structure)
7. [Detection & Counting Logic](#detection--counting-logic)
8. [Key Implementation Details](#key-implementation-details)
9. [Phase 4 Roadmap](#phase-4-roadmap)
10. [Known Issues & Solutions](#known-issues--solutions)
11. [Quick Start Guide](#quick-start-guide)

---

## Project Overview

### Problem Statement

Manual counting of African catfish fingerlings on father's farm is:
- Time-consuming (1-2kg sample counted one-by-one)
- Labor-intensive
- Prone to errors
- Limits scalability

### Solution

Native Android app using computer vision to automate fish counting via smartphone camera.

### Physical Setup

```text
┌─────────────────────────────────────┐
│  Smartphone (mounted above)         │
│         ↓ Camera                    │
└─────────────────────────────────────┘
│
↓
┌─────────────────────────────────────┐
│  Acrylic Channel (3-5cm wide)       │
│  ← LED backlight                    │
│  → → → Fish flow (single file)      │
│  Water flow: 1-3 fish/second        │
└─────────────────────────────────────┘
```

**Detection Method:** Dark fish shapes on bright LED background

---

## System Architecture

### MVVM Pattern

```text
┌─────────────────────────────────────────────────────┐
│                      View Layer                      │
│  (Jetpack Compose - UI Components)                  │
│                                                      │
│  - CameraScreen.kt                                  │
│  - CameraPreview.kt (integrated)                    │
│  - ProcessedImageView.kt                            │
└──────────────────┬──────────────────────────────────┘
│ observes StateFlow
│ calls methods
┌──────────────────▼──────────────────────────────────┐
│                  ViewModel Layer                     │
│  (Business Logic & State Management)                │
│                                                      │
│  - CameraViewModel.kt                               │
│    • Manages UI state (CameraUiState)               │
│    • Coordinates processing                         │
│    • Handles frame pipeline                         │
└──────────────────┬──────────────────────────────────┘
│ uses
┌──────────────────▼──────────────────────────────────┐
│                   Model Layer                        │
│  (Data & Domain Logic)                              │
│                                                      │
│  Repository:                                        │
│  - CameraRepository.kt (CameraX operations)         │
│                                                      │
│  Domain Processing:                                 │
│  - ImageProcessor.kt (OpenCV operations)            │
│  - FishBlob.kt (Data model for detected fish)       │
│  - FishTracker.kt (Identity tracking logic)         │
│                                                      │
│  Utilities:                                         │
│  - ImageConverter.kt (format conversions & stats)   │
│  - ProcessingConfig.kt (centralized configuration)  │
└─────────────────────────────────────────────────────┘
```

---

## Technology Stack

### Core Technologies

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM
- **Camera:** CameraX (`androidx.camera:camera-*`)
- **Computer Vision:** OpenCV 4.10.0 (`org.opencv:opencv`)

---

## Development Progress

### Phase 1: Camera Basics ✅ COMPLETED

- ✅ Task 1.1: Setup CameraX dependencies
- ✅ Task 1.2: Request camera permissions (runtime)
- ✅ Task 1.3: Implement camera preview with `PreviewView`
- ✅ Task 1.4: Add camera start/stop controls
- ✅ Task 1.5: Handle lifecycle properly (`DisposableEffect`)

---

### Phase 2: OpenCV Integration & Optimization ✅ COMPLETED

- ✅ Task 2.1: Add OpenCV dependency
- ✅ Task 2.2: Initialize OpenCV in `FishCounterApplication`
- ✅ Task 2.3: Setup `ImageAnalysis` use case
- ✅ Task 2.4: Optimized `ImageProxy` → `Mat` conversion (Direct YUV → BGR)
- ✅ Task 2.5: Integrated `ProcessingConfig` for global parameter tuning
- ✅ Task 2.6: Implemented conversion error tracking and statistics
- ✅ Task 2.7: Cleaned up and documented `CameraViewModel`
- ✅ Task 2.8: Applied `ResolutionSelector` in `CameraRepository` for 640x480 targets

---

### Phase 3: Fish Detection ✅ COMPLETED

- ✅ Task 3.1: Capture background reference
- ✅ Task 3.2: Background subtraction
- ✅ Task 3.3: Binary thresholding (Parameterized)
- ✅ Task 3.4: Morphological operations (Parameterized)
- ✅ Task 3.5: Blob detection (Find contours)
- ✅ Task 3.6: Define counting line
- ✅ Task 3.7: Fish tracking (Centroid tracking with unique IDs)
- ✅ Task 3.8: Display results and testing (Total counter & Line crossing)

**Status:** ACHIEVED 100% Accuracy (30/30) in high-speed, tight-cluster simulator testing.

---

## Detection & Counting Logic

### 1. The Pipeline
1.  **Grayscale Conversion**: Input frame converted to grayscale.
2.  **Background Subtraction**: `absdiff` calculated between live frame and stored reference.
3.  **Thresholding**: Binary mask created using a high-selectivity threshold (65.0).
4.  **ROI Masking**: Left and Right edges (18% each) are blacked out to eliminate boundary noise.
5.  **Noise Filtering**: Median Blur (3x3) and Morphological Open (1 iteration) remove small speckles.
6.  **Separation (Erosion)**: Morphological Erode (2 iterations) shrinks blobs to physically "cut" thin bridges between nearby fish.
7.  **Contour Analysis**: `findContours` extracts remaining white blobs.
8.  **Tracking**: Match blobs to IDs using **Bounding Box Overlap** (primary) and Centroid Distance (fallback).
9.  **Counting**: A Three-Zone State Machine tracks fish from entry (Above Line) to transit (Counting Band) to exit (Below Line).

### 2. Tuned Parameters (Ultimate Separation)
| Parameter | Value | Purpose |
| :--- | :--- | :--- |
| `SUBTRACTION_THRESHOLD` | 65.0 | High selectivity for solid white fish objects. |
| `GAUSSIAN_BLUR_SIZE` | 0.0 | Razor-sharp edges to prevent smearing close objects. |
| `MORPH_ERODE_ITERATIONS` | 2 | Physically separates "kissing" or tight-packed fish. |
| `TRACKING_MAX_LOST_FRAMES` | 1 | Immediate ID release to prevent ID stealing in clusters. |
| `TRACKING_MIN_STABILITY_FRAMES` | 1 | Instant trust for detections in clean-mask environments. |
| `ROI_SIDE_PERCENT` | 0.18 | Dead-zone for sensor/simulator edge noise. |

---

## Key Implementation Details

### 1. Robust Tracking (`FishTracker.kt`)
Uses **Bounding Box Overlap** as the primary matching strategy. This is superior to centroid distance for large or wobbling objects because boxes will likely overlap even if the center jumps.

### 2. State-Based Counting
Instead of simple "Frame A vs Frame B" logic, each fish has a state (`canBeCounted`).
- **Eligibility**: Granted only if the fish first appears clearly above the counting line.
- **Trigger**: Counter increments only when an eligible fish passes at least `8px` (`COUNTING_BAND_HEIGHT`) below the line.

---

## Phase 4 Roadmap (Planned)

1.  **Session Management**: Saving counts to local storage with timestamps.
2.  **UI/UX Overhaul**: Themed interface, history view, and count sharing.
3.  **Calibration UI**: Sliders to adjust ROI and Counting Line in real-time.
4.  **Field Testing**: Transitioning from simulator to real-world footage.

---

## Known Issues & Solutions

- **Over-Smoothing**: Resolved by setting blur to 0 and using erosion to maintain gaps.
- **ID Stealing**: Resolved by lowering lost-frame persistence to 1.
- **Hollow Centers**: Resolved by high-contrast thresholding and contour-based filtering.

---

*End of Documentation*
