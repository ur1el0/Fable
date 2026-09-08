# ADR 003: Modular Source Extractor Architecture

## Status
Accepted

## Context
Web novel and serialized fiction hosting websites frequently alter their CSS selectors, update URL schemes, or introduce anti-bot countermeasures. Writing ad-hoc scraping logic directly inside repositories or UI components creates fragile coupling where a minor remote HTML redesign can break core application stability.

## Decision
We chose a **Modular Source Extractor Architecture** anchored by a universal domain contract (`Source`):
- Every external website scraper is encapsulated in an independent extractor class implementing standardized methods (`getPopularBooks`, `searchBooks`, `getChapterList`, `getChapterContent`).
- CSS selectors, pagination parameters, and source-specific request headers are strictly contained within the respective extractor implementation.
- The core application interacts exclusively with the generic `SourceRepository` and domain `Book` / `Chapter` models.

## Consequences
- **Positive:** Perfect isolation; a broken extractor or 404 error on a remote website cannot crash the local library, reader engine, or Room database.
- **Positive:** Enables automated unit testing against static HTML fixtures (`src/test/resources/fixtures/`) without requiring live network calls.
- **Positive:** Lays the foundation for a future dynamic extension ecosystem in Phase 7.
