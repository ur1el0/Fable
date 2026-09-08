# ADR 007: Coil Image Caching Pipeline & Memory Limits

## Status
Accepted

## Context
An e-reader application displays dozens of book cover illustrations simultaneously in library grids, source discovery catalogs, and reading history lists. Loading unbounded high-resolution external images into memory risks rapid garbage collection churn, dropped frames during scrolling, and fatal Out-Of-Memory (`java.lang.OutOfMemoryError`) crashes on lower-end Android hardware.

## Decision
We chose **Coil (Coroutine Image Loader)** as the standardized image loading pipeline, configured with strict memory and disk cache bounds:
- **Bounded Memory Cache:** Coil's memory cache is strictly capped at **20% of available application heap** (`maxSizePercent(0.20)`).
- **Dedicated Disk Cache:** Bounded disk cache in internal storage (`context.cacheDir/image_cache`) capped at 100 MB with LRU eviction.
- **Adaptive Downsampling:** Compose `AsyncImage` instances must specify target view dimensions and utilize `ContentScale.Crop` to prevent decoding full-resolution bitmaps when rendering 120x180dp cards.

## Consequences
- **Positive:** Guaranteed immunity from image-induced OOM crashes; low heap footprint.
- **Positive:** Smooth 60fps scrolling in the Library grid due to asynchronous bitmap decoding on background dispatchers.
- **Negative:** Extremely large cover art or illustrations are downscaled to thumbnail dimensions to fit cache budgets.

