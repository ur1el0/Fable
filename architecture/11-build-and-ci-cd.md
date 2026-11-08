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

Every Pull Request and push to a feature branch is validated through an automated CI pipeline (`.github/workflows/ci.yml`):

```yaml
name: Android CI Quality Gates

on:
  push:
    branches: [ feature/*, fix/* ]
  pull_request:
    branches: [ main ]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant Execute Permission for Gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Run Android Lint
        run: ./gradlew lintDebug

      - name: Compile Debug APK
        run: ./gradlew assembleDebug
```

