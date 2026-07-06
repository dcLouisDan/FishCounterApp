# Throughput Count Estimator Design

## Purpose

The app needs to count fingerlings flowing through a tube or funnel at a fast pace. In that setup, fingerlings may bunch together, vary in size, rotate, and produce merged blobs. The counting model must therefore stop assuming that every detected blob equals exactly one fish.

## Scope

Add a domain-only `CountEstimator` that estimates how many fingerlings a detected blob represents. This first slice does not replace the live camera counter. It creates a tested component that can later be integrated into `FishTracker` or a tube-gate counting mode.

## Model

The estimator uses calibrated average fingerling area as its primary signal. It also uses blob width relative to the expected tube width as a secondary signal, because a wide merged blob at the tube exit likely represents multiple fish passing together. The final estimate is the larger of the area-based and width-based estimates, clamped to a configured maximum.

## Inputs

- `FishBlob`: detected blob with area and bounding box.
- `CountEstimatorConfig`: average fish area, tube width, minimum area ratio, maximum fish per blob, and confidence thresholds.

## Outputs

- `EstimatedCount`: estimated fish count and confidence.

## Confidence

Confidence starts high for single-fish-sized blobs, medium for plausible merged blobs, and low when the blob is too small or the estimate hits the configured maximum. Low confidence does not prevent counting; it identifies cases that should eventually be surfaced in UI or logs for tuning.

## Verification

Add JVM tests for single fish, tiny noise, area-based merged blobs, width-based merged blobs, size variation, and maximum clamp behavior. Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.
