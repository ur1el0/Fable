# 11 Build System & CI/CD Pipeline

## 1. Gradle Build Configuration

Fable uses **Gradle Kotlin DSL** (`build.gradle.kts`) paired with Gradle Version Catalogs (`gradle/libs.versions.toml`) to enforce centralized, type-safe dependency management.

### Build Configuration Baselines
- **Android Gradle Plugin (AGP):** 8.x+ / 9.x
- **Kotlin Version:** 2.2+ with Compose Compiler Plugin
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK & Compile SDK:** 36 (Android 16)
- **Java Compatibility:** Java 11 / Java 17 toolchain

---

## 2. ProGuard & R8 Code Optimization

Release builds enable R8 code and resource shrinking to reduce APK footprint and strip sensitive debugging statements:

The project currently uses AGP 9.2.1, so it uses the legacy build-type flags. The simplified `optimization { enable = true }` DSL is available starting in AGP 9.3.

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/rules.keep"
            )
        }
    }
}
```

### Project R8 Rules (`app/src/main/keepRules/rules.keep`)
```proguard
# Strip Android debug and verbose log statements in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

Room dependencies provide their generated database keep rules through their consumer rules; avoid duplicating broad application-level keep rules.

---

## 3. GitHub Actions Continuous Integration (CI)

The Android CI workflow lives at `.github/workflows/ci.yml`. It runs for pull requests targeting `main`, pushes to `main`, and manual dispatches. On an Ubuntu 24.04 runner with Java 17 and Gradle dependency caching, it:

- Runs `:app:testDebugUnitTest` and `:app:lintDebug`.
- Builds the debug APK and compiles Android-test Kotlin sources. Instrumentation tests are not run because this workflow does not start an emulator.
- Builds the release APK with a reserved HTTPS URL, so release configuration is checked without requiring a secret or contacting a live backend.

