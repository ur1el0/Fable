# 10 Testing & Quality Assurance (ISO/IEC 25010)

## 1. Multi-Tier Verification Matrix

Fable Android enforces deterministic automated verification across JVM unit tests, Robolectric/Compose component tests, Room SQLite migration tests, Android Lint, and device/emulator instrumentation tests:

| Test Suite | Location | What It Verifies |
|---|---|---|
| **API & Transport Tests** | `FableApiServiceTest`, `MangaApiServiceTest`, `ServerUrlValidatorTest` | REST path/method contracts (`GET`, `POST`, `PATCH`, `DELETE`), Standard Ebooks & Gutenberg normalization, MangaDex URL sanitization, coroutine-to-OkHttp call cancellation, and URL validation rules. |
| **Repository & Data Integrity Tests** | `RoomStoryRepositoryComicTest`, `RoomStoryRepositoryManagementTest`, `RemoteAccountRepositoryTest`, `SharedPreferencesReaderPreferencesRepositoryTest` | Parallel fault-isolated catalog refresh, non-destructive `coalesceChapters`, story edit/delete, explicit offline chapter download/removal, account deletion purge, 401 session reset, and preference persistence. |
| **Reader & Annotation Engine Tests** | `ReaderTextPaginatorTest`, `ReaderProgressMapperTest`, `InBookSearchAndHighlightTest`, `ComicZoomMathTest`, `MangaChapterReaderTest` | Word-offset pagination, progress preservation across font/layout changes, cross-chapter search snippets, word-range highlight slicing, Markdown/text highlight export, comic zoom/pan clamping, and webtoon/manga Compose UI rendering. |
| **ViewModel & Navigation Tests** | `FableNavigationTest`, `CatalogViewModelTest`, `ExploreViewModelTest`, `ReaderViewModelTest`, `StoryEditorViewModelTest`, `WriterDraftViewModelTest`, `AccountViewModelTest`, `ServerConfigViewModelTest` | Typed back-stack navigation/savers, debounced search cancellation, full-story hydration prior to editing, offline download UI state transitions, draft auto-save, and account lifecycle states. |
| **Android Instrumentation & Room Migrations** | `app/src/androidTest/.../ShelfStateMigrationTest`, `FableAppSmokeTest` | Real SQLite schema upgrades across `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`, and `MIGRATION_5_6`, plus Compose app launch smoke verification on a running Android runtime. |

---

## 2. Standard Verification Commands

```bash
# 1. Full JVM unit test suite (115 tests), Android Lint, Debug APK, and AndroidTest compilation
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin

# 2. Release build validation (HTTPS URL gate + lintVitalRelease + R8 minification)
./gradlew :app:assembleRelease -PfableReleaseApiBaseUrl=https://api.example.invalid/api/v1/

# 3. Instrumentation tests on a connected device or running emulator
./gradlew :app:connectedDebugAndroidTest
```

> **Verification Honesty Rule:** `:app:compileDebugAndroidTestKotlin` verifies that instrumentation tests compile, whereas `:app:connectedDebugAndroidTest` executes them on an active Android device or emulator. Always report the two distinctly.
