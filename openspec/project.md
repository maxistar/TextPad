# Project Context

## Purpose
Simple Text Editor for Android focused on editing small plain-text files and quick notes. Keep the app lightweight, reliable, and easy to use.

## Tech Stack
- Android app (Android SDK)
- Java (primary language)
- Gradle (Groovy) build system
- AndroidX (appcompat, test libraries)
- JUnit 5 for unit tests; Espresso and AndroidX Test for instrumentation
- fastlane metadata for Play Store/F-Droid releases

## Project Conventions

### Code Style
- Java packages under `com.maxistar.textpad`
- Prefer clear, explicit naming for activities/services/utils
- Resources follow Android naming conventions (`res/values-xx/strings.xml`, `drawable-*`)

### Architecture Patterns
- Classic Android app structure with Activities + Services + utility classes
- ServiceLocator used for wiring shared services
- No dependency injection framework in use

### Testing Strategy
- Unit tests in `app/src/test` with JUnit 5
- Instrumentation tests via AndroidX Test + Espresso
- Lint run via `./gradlew lint`

### Git Workflow
- GitHub repo: https://github.com/maxistar/TextPad
- Default branch and commit conventions are standard; use PRs for changes (confirm if you have a preferred branch/commit policy)

## Domain Context
- Plain-text editor only (no rich text); small files are the primary use case
- Supports translations via Crowdin
- Distributed on Google Play and F-Droid

## Important Constraints
- minSdkVersion 21; compile/target SDK 35
- Keep app lightweight and fast; avoid heavy dependencies
- Offline-first: core editing should not require network

## External Dependencies
- Android SDK/Gradle toolchain
- Crowdin project for translations
- fastlane for store metadata
- Google Play and F-Droid distribution
