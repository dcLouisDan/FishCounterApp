# YUV Conversion Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CameraX `YUV_420_888` to NV21 packing stride-safe and covered by JVM tests.

**Architecture:** Add a pure Kotlin `Yuv420Nv21Packer` that accepts plane metadata and emits NV21 bytes. Replace the ad hoc packing code inside `ImageConverter` with calls to the packer.

**Tech Stack:** Kotlin, JUnit 4, Android CameraX `ImageProxy`, OpenCV `Mat`, Gradle `testDebugUnitTest`.

---

### Task 1: Test YUV Plane Packing

**Files:**
- Create: `app/src/test/java/com/example/fishcounterapp/utils/Yuv420Nv21PackerTest.kt`
- Create: `app/src/main/java/com/example/fishcounterapp/utils/Yuv420Nv21Packer.kt`

- [ ] **Step 1: Write failing tests**

Add tests for contiguous planes, padded row strides, pixel stride 2 chroma, and buffer position preservation.

- [ ] **Step 2: Verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.fishcounterapp.utils.Yuv420Nv21PackerTest`

Expected: compilation fails because `Yuv420Nv21Packer` does not exist.

- [ ] **Step 3: Implement packer**

Create `Yuv420Nv21Packer` with `Plane` metadata and `pack(width, height, y, u, v)`.

- [ ] **Step 4: Verify GREEN**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.fishcounterapp.utils.Yuv420Nv21PackerTest`

Expected: tests pass.

### Task 2: Wire ImageConverter

**Files:**
- Modify: `app/src/main/java/com/example/fishcounterapp/utils/ImageConverter.kt`
- Test: `app/src/test/java/com/example/fishcounterapp/utils/Yuv420Nv21PackerTest.kt`

- [ ] **Step 1: Replace direct path packing**

Use `Yuv420Nv21Packer.pack` inside `imageProxyToMatDirect`.

- [ ] **Step 2: Replace fallback path packing**

Use `Yuv420Nv21Packer.pack` inside `imageProxyToBitmapFallback`.

- [ ] **Step 3: Remove unused private packing helpers**

Delete `copyPlaneWithStride` and `deinterleavePlanes` from `ImageConverter`.

- [ ] **Step 4: Verify full tests and build**

Run: `.\gradlew.bat testDebugUnitTest`

Run: `.\gradlew.bat assembleDebug`

Expected: both commands succeed.
