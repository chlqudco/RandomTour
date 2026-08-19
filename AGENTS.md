# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project. Production Kotlin code lives in `app/src/main/java/com/chlqudco/randomtour/`. Compose screens and state coordination are in `RandomTourApp.kt` and `RandomTourViewModel.kt`; location, candidate discovery, persistence, and map integration are separated into repository or helper files. Android resources are under `app/src/main/res/`. JVM tests belong in `app/src/test/`, device tests in `app/src/androidTest/`, and portfolio screenshots in `docs/screenshots/`. Dependency versions are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Run commands from the repository root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

`assembleDebug` creates `app/build/outputs/apk/debug/app-debug.apk`. Unit tests require no device. Instrumented tests require a connected Android 12+ device or emulator. Before submitting, run the first three commands together. Use Android Studio for interactive Compose development and Logcat inspection.

## Coding Style & Naming Conventions

Use four-space indentation and standard Kotlin conventions: `PascalCase` for types and composables, `camelCase` for functions and properties, and `UPPER_SNAKE_CASE` for constants. Keep UI state immutable and update it through the ViewModel. Put reusable distance, bearing, and filtering rules outside composables. Do not add code comments or KDoc unless explicitly requested; prefer clear names and small functions. No formatter is configured, so rely on IDE Kotlin formatting and `lintDebug`.

## Testing Guidelines

JUnit 4 is used for JVM tests. Name files `*Test.kt` and tests after observable behavior, such as `walkQueryRequestsOutdoorDestinations`. Add unit tests for pure search, distance, bearing, or arrival logic. Use `androidTest` only when Android framework or Compose interaction is required. Bug fixes should include a regression test where practical.

## Commit & Pull Request Guidelines

History uses concise Korean subjects such as `1차 구현 완료` and `readme 수정`, without mandatory prefixes. Keep each commit focused and describe the outcome in one short subject. Pull requests should explain the user-facing change, verification commands, and any location or data-source implications. Link related issues and attach before/after screenshots for Compose UI changes.

## Security & Configuration

Set `NAVER_MAP_API_KEY` only in untracked `local.properties`. Never commit credentials, device coordinates, or private location screenshots. Preserve OpenStreetMap attribution when using Overpass-derived destinations.
