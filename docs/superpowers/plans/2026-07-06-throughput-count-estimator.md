# Throughput Count Estimator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tested domain component that estimates fish count for merged or bunched fingerling blobs.

**Architecture:** Create `CountEstimator`, `CountEstimatorConfig`, `EstimatedCount`, and `CountConfidence` in the existing `domain.processing` package. Keep it pure Kotlin and independent from CameraX, OpenCV image operations, and UI.

**Tech Stack:** Kotlin, JUnit 4, Gradle `testDebugUnitTest`, existing `FishBlob` model.

---

### Task 1: CountEstimator Domain Tests

**Files:**
- Create: `app/src/test/java/com/example/fishcounterapp/domain/processing/CountEstimatorTest.kt`
- Create: `app/src/main/java/com/example/fishcounterapp/domain/processing/CountEstimator.kt`

- [ ] **Step 1: Write failing tests**

Add tests showing that the estimator:
- returns 1 high-confidence count for a single-fish-sized blob
- returns 0 low-confidence count for tiny noise
- estimates merged blobs from area
- estimates merged blobs from tube-width occupancy
- tolerates moderate single-fish size variation
- clamps estimates to the configured maximum

- [ ] **Step 2: Verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.fishcounterapp.domain.processing.CountEstimatorTest`

Expected: compilation fails because `CountEstimator` does not exist.

- [ ] **Step 3: Implement minimal estimator**

Create `CountEstimator.kt` with the config, result, confidence enum, and `estimate(blob)` method.

- [ ] **Step 4: Verify GREEN**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.fishcounterapp.domain.processing.CountEstimatorTest`

Expected: all count estimator tests pass.

### Task 2: Full Verification

**Files:**
- Verify: all changed files

- [ ] **Step 1: Run full JVM tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: all unit tests pass.

- [ ] **Step 2: Run debug build**

Run: `.\gradlew.bat assembleDebug`

Expected: debug build succeeds.
