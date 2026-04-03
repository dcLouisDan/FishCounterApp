# Fish Counter Mobile App - Project Documentation

**Last Updated:** October 26, 2024  
**Current Status:** Phase 3 In Progress (Tasks 3.1 - 3.5 Complete)  
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

### Phase 3: Fish Detection 🔄 IN PROGRESS

- ✅ Task 3.1: Capture background reference
- ✅ Task 3.2: Background subtraction
- ✅ Task 3.3: Binary thresholding (Parameterized)
- ✅ Task 3.4: Morphological operations (Parameterized)
- ✅ Task 3.5: Blob detection (Find contours)
- ⏳ Task 3.6: Define counting line
- ⏳ Task 3.7: Fish tracking (assign IDs, track movement)
- ⏳ Task 3.8: Display results and testing

**Current Performance:** 18-22 FPS (with subtraction & detection active).

---

## Code Structure

### File Organization (Current)

```text
app/src/main/java/com/example/fishcounterapp/
│
├── FishCounterApplication.kt            # Application class (OpenCV init)
├── AppContainer.kt                      # Dependency injection container
│
├── camera/
│   ├── data/
│   │   └── CameraRepository.kt          # CameraX setup (ResolutionSelector)
│   ├── ui/
│   │   ├── CameraScreen.kt              # UI Entry point & Status Indicators
│   │   ├── CameraControls.kt            # New buttons for BG capture & Subtraction
│   │   └── ProcessedImageView.kt        # Display component
│   └── viewmodel/
│       └── CameraViewModel.kt           # Business logic & Pipeline orchestration
│
├── domain/
│   └── processing/
│       ├── ImageProcessor.kt            # OpenCV operations (BG Subtraction, Detection)
│       └── FishBlob.kt                  # Fish data model
│
└── utils/
    ├── ImageConverter.kt                # Optimized YUV→Mat & Stats tracking
    ├── ProcessingConfig.kt              # Central configuration constants
    └── ViewModelUtils.kt                # Factory helpers
```

---

## Key Implementation Details

### 1. Centralized Configuration (`ProcessingConfig.kt`)
All tunable parameters (Resolution, Thresholds, Area filters, Kernel sizes) are managed here, ensuring consistency across the app.

### 2. High-Performance Conversion (`ImageConverter.kt`)
- **Direct YUV → Mat**: Bypasses Bitmaps to save ~15-20ms per frame.

### 3. Fish Detection Pipeline (`ImageProcessor.kt`)
- **Background Subtraction**: `absdiff` between live feed and reference.
- **Masking**: Binary thresholding and morphological `OPEN` operation.
- **Contour Analysis**: `findContours` + area filtering + centroid calculation.
- **Visual Feedback**: Real-time green bounding boxes and centroids drawn on the UI.

---

## Phase 3 Roadmap (Planned Implementation)

*Reference for upcoming tasks:*

1. **Counting Line**: Defining a virtual crossing point in the frame.
2. **Tracking**: Assigning IDs and tracking centroids across consecutive frames.
3. **Counter Logic**: Incrementing total count when a fish crosses the line.

---

## Known Issues & Solutions

- **Memory Management**: Strictly using `try-finally` with `mat.release()` and `imageProxy.close()`.
- **Blob Stability**: Area filtering (100-5000px) used to filter noise; may require field-tuning.

---

*End of Documentation*
