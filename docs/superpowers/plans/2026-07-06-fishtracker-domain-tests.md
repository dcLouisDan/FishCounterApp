# FishTracker Domain Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add focused JVM unit tests that protect the fish-counting tracker behavior.

**Architecture:** Create one unit test class for `FishTracker` using real `FishBlob` values. Keep production code unchanged unless the tests expose a specific behavior defect.

**Tech Stack:** Kotlin, JUnit 4, Gradle `testDebugUnitTest`, OpenCV `Point` and `Rect` data classes.

---

### Task 1: FishTracker Counting Tests

**Files:**
- Create: `app/src/test/java/com/example/fishcounterapp/domain/processing/FishTrackerTest.kt`
- Test: `app/src/test/java/com/example/fishcounterapp/domain/processing/FishTrackerTest.kt`

- [ ] **Step 1: Add tests for core tracker behavior**

Create `FishTrackerTest.kt` with tests for one-time counting, below-line spawn rejection, stable ID matching, and lost-track removal.

- [ ] **Step 2: Run tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: all JVM unit tests pass.

- [ ] **Step 3: Verify build**

Run: `.\gradlew.bat assembleDebug`

Expected: debug APK build succeeds.

- [ ] **Step 4: Review diff**

Run: `git diff -- docs/superpowers/specs/2026-07-06-fishtracker-domain-tests-design.md docs/superpowers/plans/2026-07-06-fishtracker-domain-tests.md app/src/test/java/com/example/fishcounterapp/domain/processing/FishTrackerTest.kt`

Expected: only the spec, plan, and tracker test file are changed.
