# Fable

An offline-first Android literature and manga reader built with Kotlin and Jetpack Compose.

Fable brings the iOS reader experience to Android, including curated story discovery, a personal shelf, prose and manga reading, and community publishing. The Android client keeps its local library and reading state available offline while integrating with the shared FastAPI backend.

## Status

> 🚧 In development

## Planned Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Database:** Room (SQLite)
* **Networking:** OkHttp and kotlinx.serialization
* **Backend:** Shared FastAPI service from Fable-IOS
* **Image Loading:** Coil
* **Background Tasks:** WorkManager

## Planned Features

* Offline-first experience
* Local library management
* Curated stories, Project Gutenberg, and manga discovery
* Local shelf, bookmarks, progress, and offline chapter cache
* Community accounts and story publishing
* Prose and manga reading modes

## Roadmap

* **Fable Android:** Native companion to Fable-IOS, with Room-backed offline state and the shared catalog, account, and shelf APIs.

## License

MIT
