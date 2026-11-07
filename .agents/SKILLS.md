# Fable - Operational Playbooks & Engineering Commands

This document contains standard commands, development workflows, and operational playbooks across the Fable Android ecosystem (Kotlin, Jetpack Compose, Room, WorkManager, Gradle).

---

## 1. Gradle Build & Quality Commands

Always run Gradle commands via the project root wrapper (`./gradlew`).

### Compiling & Assembling
- **Compile Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Compile Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```
- **Clean Build Artifacts:**
  ```bash
  ./gradlew clean
  ```

### Testing & Verification
- **Run Unit Tests (JVM):**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Run Static Analysis & Lint Checks:**
  ```bash
  ./gradlew lintDebug
  ```
- **Run Connected Instrumented Tests (Requires connected emulator or device):**
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

---

## 2. ADB Device Management & Debugging Playbook

### Device Inspection & APK Deployment
1. **List Connected Devices:**
   ```bash
   adb devices
   ```
2. **Install Debug APK directly:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Launch MainActivity:**
   ```bash
   adb shell am start -n com.example.fable/.MainActivity
   ```
4. **Kill & Reset App State (Simulate Fresh Install):**
   ```bash
   adb shell pm clear com.example.fable
   ```

### Real-Time Logcat Monitoring
Filter logs specifically for Fable subsystem tags and fatal runtime exceptions:
```bash
adb logcat -v time -s Fable:V FableExtractor:D FableDownload:D AndroidRuntime:E
```

---

## 3. Room Database Inspection Playbook

### Inspecting Local SQLite Database from Device
Pull the internal database file to the host machine for inspection with `sqlite3`:
```bash
adb exec-out run-as com.example.fable cat databases/fable.db > /tmp/fable_debug.db
sqlite3 /tmp/fable_debug.db ".tables"
sqlite3 /tmp/fable_debug.db "SELECT id, title, source FROM books;"
```

### Exporting Room Schema
When modifying Room entities, ensure the schema JSON is exported to `app/schemas/` to track migration history.

---

## 4. WorkManager Background Job Diagnostics

### Inspecting WorkManager Internal State
Dump scheduled WorkManager tasks to terminal:
```bash
adb shell dumpsys jobscheduler | grep com.example.fable
```

### Triggering Immediate WorkManager Execution
Force an enqueued download worker to execute immediately regardless of constraints:
```bash
adb shell cmd jobscheduler run -f com.example.fable <JOB_ID>
```

---

## 5. Pre-Push Verification Checklist

Execute this verification matrix before pushing any feature branch or opening a Pull Request:
1. **Unit Test Matrix:** `./gradlew testDebugUnitTest` (Must pass 100% with zero regressions).
2. **Static Analysis:** `./gradlew lintDebug` (Zero high-priority errors or warnings).
3. **Compilation Check:** `./gradlew assembleDebug` (Build finishes successfully).
4. **Git Cleanliness:** `git status` confirms no untracked `.apk`, `.aab`, `local.properties`, or `.hprof` heap dumps.

