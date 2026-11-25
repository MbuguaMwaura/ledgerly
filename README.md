# Ledgerly

A personal finance tracker for Android and iOS: log spending in seconds, keep budgets, track who owes you and whom you owe, and build the habit with streaks.

The business logic is shared Kotlin Multiplatform code. Each platform has its own native UI: Jetpack Compose on Android, SwiftUI on iOS (so iOS gets Apple's Liquid Glass).

## Layout

| Path | What it is |
| --- | --- |
| `shared/` | KMP module: domain, rules, persistence and one ViewModel per screen. Built for iOS as `Shared.framework` with [SKIE](https://skie.touchlab.co). |
| `androidApp/` | Android app in Jetpack Compose and Material 3. |
| `iosApp/` | iOS app in SwiftUI. The Xcode project is generated from `iosApp/project.yml` by XcodeGen. |
| `designTokens/` | `tokens.json` from the Ledgerly Clean design system: the one source for colours, type, spacing and radii on both platforms. |
| `build-logic/` | Gradle plugins, including the token code generator. |

`shared` never depends on either UI. Android reads ViewModel state with `collectAsStateWithLifecycle()`; iOS wraps each ViewModel in a small `@Observable` class (see `iosApp/Ledgerly/Home/HomeModel.swift`).

## Requirements

- JDK 17 or newer
- Android Studio (Android SDK 36), min SDK 26
- For iOS: a Mac with Xcode 26 and [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`). Deployment target iOS 17; Liquid Glass shows on iOS 26 and later.

## Build and run

```sh
# Shared tests
./gradlew :shared:testDebugUnitTest          # JVM
./gradlew :shared:iosSimulatorArm64Test      # iOS simulator, macOS only

# Android
./gradlew :androidApp:installDebug

# iOS
xcodegen --spec iosApp/project.yml
open iosApp/Ledgerly.xcodeproj               # the build phase compiles the Kotlin framework
```

## Design tokens

Edit `designTokens/tokens.json`, then:

```sh
./gradlew :designTokens:generateSwiftTokens  # rewrites iosApp/Ledgerly/DesignSystem/Generated/LedgerlyTokens.swift
```

Android regenerates its Kotlin theme constants on every build. CI fails if the Swift file is out of date.
