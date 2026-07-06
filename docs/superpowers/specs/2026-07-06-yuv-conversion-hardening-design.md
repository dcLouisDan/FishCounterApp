# YUV Conversion Hardening Design

## Purpose

`ImageConverter.imageProxyToMatDirect` currently assumes favorable YUV plane layouts in several places. Android `YUV_420_888` frames can vary by row stride, pixel stride, and buffer position, so conversion needs a small tested packing unit before OpenCV receives the frame.

## Scope

Create a pure Kotlin packer that converts Y, U, and V plane metadata into NV21 bytes. Wire `ImageConverter` to use it for the direct OpenCV path and the JPEG fallback path.

## Architecture

Add `Yuv420Nv21Packer` under `utils`. It exposes a small data class for plane metadata and a `pack` function returning a `ByteArray` in NV21 layout. The packer reads source bytes with absolute `ByteBuffer.get(index)` so caller buffer positions are preserved.

## Behaviors Covered

- Copies Y luma using image width and height, ignoring row padding.
- Packs chroma as interleaved VU bytes for NV21.
- Handles chroma planes with pixel stride 1 or 2.
- Handles row stride larger than visible chroma width.
- Preserves incoming buffer positions.

## Verification

Add JVM unit tests for the packer. Then run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.
