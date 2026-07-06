# FishTracker Domain Tests Design

## Purpose

The fish counter app's core value depends on correctly tracking detected blobs and incrementing the total only when a fish crosses the configured counting line. The current project builds, but its tests are template-only and do not protect the counting behavior.

## Scope

Add focused JVM unit tests for `FishTracker`. The first pass verifies existing domain behavior without changing production code unless a test exposes a concrete behavior gap.

## Behaviors Covered

- A fish that starts above the line and moves below the counting band increments the count once.
- A fish that first appears below the counting band is ignored and does not count.
- A counted fish is not counted again while it remains tracked across later frames.
- A fish keeps the same tracking ID across small frame-to-frame movement.
- A lost track is removed after `ProcessingConfig.TRACKING_MAX_LOST_FRAMES`.

## Architecture

The tests use real `FishTracker` and `FishBlob` instances with simple OpenCV `Point` and `Rect` values. They avoid CameraX, Android UI, OpenCV image processing, and mocks so failures point directly at tracking-state behavior.

## Verification

Run `.\gradlew.bat testDebugUnitTest` after adding the tests. Run `.\gradlew.bat assembleDebug` as a compile/package check before closing the task.
