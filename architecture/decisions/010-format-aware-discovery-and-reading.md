# ADR 010: Format-Aware Discovery and Comic Reading

## Status

Accepted

## Context

Fable serves prose books and image-based comics through the shared backend. A prose flow with adjustable typography and scrolling or pagination does not fit multi-page comics, while rendering every page of a chapter in one Compose column can retain too many image nodes on lower-memory devices. Gutenberg also exposes catalog, popular, and recent sorts through its API, so sorting a partial local cache cannot produce the same results as the provider catalog.

## Decision

- Keep provider sorting in the shared API contract. Android passes the selected Gutenberg sort to the backend and uses Room-cached stories while a request is in flight or unavailable.
- Keep prose rendering in its text reader. Route manga chapters to a dedicated reader with a lazy vertical page list for webtoon-style reading or a one-page horizontal pager for page-turn reading, including left-to-right and right-to-left directions.
- Persist the manga reading mode through the existing reader-preferences repository. Do not change the stored story or chapter schema for this preference.
- Debounce page-progress writes while reading and save the visible position when leaving a comic chapter, reducing repeated shelf synchronization while retaining the latest location.

## Consequences

- Recent and popular discovery reflects backend ordering rather than client-side ordering of an incomplete cache.
- Only visible and nearby comic pages need to be composed by the reader; the previous eager fixed-height image stack is removed from the prose chapter renderer.
- A user can keep one comic reading mode across launches without changing the prose reader's typography or layout preferences.
- Device-level memory, frame pacing, chapter transitions, and offline image behavior still require manual validation on representative Android hardware before performance claims are made.
