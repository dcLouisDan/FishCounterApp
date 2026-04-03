# Fish Counter Mobile App - Project Documentation

**Last Updated:** October 26, 2024  
**Current Status:** Phase 3 Complete (Core Logic) - Refinement In Progress  
**Developer:** Dan (BS IT, Junior Software Developer, Pampanga, Philippines)

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Technology Stack](#technology-stack)
4. [Development Progress](#development-progress)
5. [Performance Benchmarks](#performance-benchmarks)
6. [Code Structure](#code-structure)
7. [Key Implementation Details](#key-implementation-details)
8. [Phase 3 Roadmap](#phase-3-roadmap)
9. [Known Issues & Solutions](#known-issues--solutions)
10. [Quick Start Guide](#quick-start-guide)

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

### Phase 3: Fish Detection ✅ CORE COMPLETE / 🔄 REFINEMENT

- ✅ Task 3.1: Capture background reference
- ✅ Task 3.2: Background subtraction
- ✅ Task 3.3: Binary thresholding (Parameterized)
- ✅ Task 3.4: Morphological operations (Parameterized)
- ✅ Task 3.5: Blob detection (Find contours)
- ✅ Task 3.6: Define counting line
- ✅ Task 3.7: Fish tracking (Centroid tracking with unique IDs)
- ✅ Task 3.8: Display results and testing (Total counter & Line crossing)

**Current Status:** The core pipeline is finished. Current focus is on refining counting consistency and resolving simulator-specific motion artifacts.

---

## Key Implementation Details

### 1. Centralized Configuration (`ProcessingConfig.kt`)
All tunable parameters (Thresholds, Blur sizes, Tracking distances, Area filters) are managed here.

### 2. Detection & Tracking Pipeline
- **Preprocessing**: Gaussian Blur (7x7) on background and frames to reduce noise.
- **Subtraction**: `absdiff` + thresholding.
- **Post-processing**: Median Blur (5x5) + Morphological Open/Close to clean the mask.
- **Tracking**: Robust tracker (`FishTracker.kt`) uses both **Bounding Box Overlap** and Centroid Distance.
- **Counting**: Detects when a validated fish track (seen for 3+ frames) crosses the line downward.

---

## Known Issues & Solutions (Current Focus)

### 1. Counting Inconsistency
**Problem:** The system occasionally double-counts or misses fish. Detection is stable for large ovals but can still be fragmented by high-frequency "wobble" noise in the simulator.
**Current Fixes:**
- **Box Overlap Matching:** Prioritizes overlapping boxes to keep IDs stable for large objects.
- **Stability Requirement:** Only counts fish that have been tracked for at least 3 consecutive frames.
- **Origin Validation:** Requires fish to originate above a "safe zone" before the counting line.
- **Directional Enforcement:** Only counts top-to-bottom crossings.
**Next Steps:**
- Tune `TRACKING_MAX_LOST_FRAMES` to handle temporary disappearances without ID reset.
- Refine `BLOB_MERGE_MAX_DISTANCE_Y` to prevent nearby fish from merging into a single count.

---

*End of Documentation*
