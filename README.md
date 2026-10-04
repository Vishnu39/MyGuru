# VocabTrackerKMP (MyGuru)

**MyGuru** is a Kotlin Multiplatform (KMP) & Compose Multiplatform application for Android and iOS designed to help users learn and retain German-English vocabulary using an adaptive **Leitner Spaced Repetition System**.

---

## 🌟 Key Features

- **Spaced Repetition Engine (Leitner System)**:
  - 5 progression box levels with exponential review intervals (1 day, 3 days, 7 days, 14 days, 30 days).
  - Correct answers advance words to the next box level (marking words as **Mastered** after Box 5).
  - Incorrect answers demote words back to Box 1 for re-practice during the current study session.

- **Interactive 3D Flashcards**:
  - **Flip Gesture**: Tap flashcards to perform a 180° 3D card flip revealing the translation and context.
  - **Swipe Controls**: Swipe Right ("Got it") or Swipe Left ("Repeat") to grade recall performance.

- **Offline-First Storage**:
  - Powered by **Room KMP** (`androidx.room`) with bundled SQLite (`sqlite-bundled`).
  - Persistent progress tracking across application restarts and platform updates.

- **Remote Cloud Sync**:
  - Downloads and merges new vocabulary lists from remote JSON API endpoints via **Ktor Client** and **kotlinx.serialization** without disrupting local study progress.

- **Cross-Platform Shared UI**:
  - Built entirely with **Compose Multiplatform** and **Material 3** for consistent UI and smooth animations across Android & iOS.

---

## 🏗️ Project Architecture & Tech Stack

```text
VocabTrackerKMP/
├── androidApp/               # Native Android application entry point & Manifest
├── iosApp/                   # Xcode project & SwiftUI root hosting Compose UI
└── shared/                   # KMP Module (Shared Logic & UI)
    ├── commonMain/
    │   └── kotlin/com/vish/myguru/
    │       ├── core/
    │       │   ├── database/   # Room Database, DAO (WordDao), and WordEntity
    │       │   └── designsystem/# Material 3 AppTheme, AppColors, AppTypography
    │       ├── di/             # Koin Dependency Injection module (appModule)
    │       └── features/
    │           └── vocabulary/ # Models, LeitnerEngine, Repository, ViewModel & Compose UI
    ├── androidMain/            # Android platform implementations (Database builder, Application)
    └── iosMain/                # iOS platform implementations (MainViewController, Database builder)
```

### Tech Stack

| Component | Library / Framework |
| :--- | :--- |
| **Language** | Kotlin 2.4.x |
| **UI Framework** | Compose Multiplatform (Material 3) |
| **Architecture** | MVVM / Clean Architecture |
| **Database** | Room KMP (`androidx.room:room-runtime`) |
| **Networking** | Ktor Client (`io.ktor:ktor-client-core`) |
| **Serialization** | `kotlinx.serialization` |
| **Dependency Injection** | Koin Multiplatform (`io.insert-koin:koin-compose`) |
| **Async / Reactive** | Kotlin Coroutines & StateFlow |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug / 2024.2.1+ recommended) with Kotlin Multiplatform plugin.
- **Xcode** 15+ (for building the iOS application on macOS).
- **JDK 17 or JDK 21** configured in your environment or Android Studio JBR.

---

## 🛠️ Building & Running

### 1. Android App
Run from Android Studio toolbar run configurations, or execute via terminal:
```bash
./gradlew :androidApp:assembleDebug
```

### 2. iOS App
1. Open the `./iosApp` folder in **Xcode**:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
2. Select an iOS Simulator or connected iOS device.
3. Click **Run** (`Cmd + R`).

---

## 🧪 Running Tests

Execute cross-platform unit and integration tests:

- **Android Unit Tests**:
  ```bash
  ./gradlew :shared:testAndroidHostTest
  ```
- **iOS Simulator Tests**:
  ```bash
  ./gradlew :shared:iosSimulatorArm64Test
  ```

---

## 📄 License

This project is maintained by [Vishnu CR](https://github.com/Vishnu39).