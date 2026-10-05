# PocketGauge 📏📱

PocketGauge is a modern, feature-rich Android measurement and reference utility application built entirely with **Jetpack Compose** and **Material 3**. Designed for precision, speed, and ease of use, PocketGauge turns your Android device into a handy digital measurement instrument, gauge calculator, and reference tool.

---

## ✨ Features

- **Precise Measurement Tools:** Interactive measurement canvases with geometric calculations (`LineGeometry`, scaling, and positioning).
- **Reference Presets:** Pre-configured and customizable reference objects/presets for quick calibration and comparison in both metric and imperial units.
- **Dynamic Ruler Scaling:** Accurate ruler scales supporting multiple measurement units with smooth scaling and conversion utilities (`UnitConversions`, `MeasurementFormatter`).
- **History Tracking:** Save, view, and manage past measurements with robust serialization and history codec support.
- **Material 3 & Edge-to-Edge Design:** Fully immersive edge-to-edge UI adhering to modern Android design guidelines.
- **Theme Customization:** Supports Light mode, Dark mode, System default, and Material You Dynamic Color.
- **Smooth Onboarding / Splash Screen:** Elegant entry animation leading directly into the core app experience.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose), Material 3, Animation APIs.
- **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF), Kotlin Coroutines, and `StateFlow`.
- **Language:** [Kotlin](https://kotlinlang.org/).
- **Testing:** Comprehensive unit test suite covering domain logic, geometry calculations, repository layers, and formatting (`JUnit`).
- **Build System:** Gradle with Kotlin DSL (`build.gradle.kts`).

---

## 📂 Project Structure

```tree
app/src/main/java/com/example/pocketgauge/
├── data/           # Repositories & history storage codecs
├── domain/         # Core calculators, line geometry, ruler scales, & unit conversions
├── model/          # Data models & app state representations
├── ui/             # Jetpack Compose UI screens, viewmodels, and theme
│   ├── about/      # About screen
│   ├── components/ # Reusable UI components & widgets
│   ├── history/    # History screen & logs
│   ├── measure/    # Main measurement canvases, screens, & reference editors
│   ├── settings/   # Settings & theme configuration
│   ├── splash/     # Splash screen animation
│   └── theme/      # Material 3 color schemes, typography, and shapes
└── MainActivity.kt # Entry point with edge-to-edge and dynamic theme setup
```

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer recommended)
- Android SDK API level 24 or higher (Target SDK 34/35)
- JDK 17+

### Installation & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/PocketGauge.git
   ```
2. Open Android Studio and select **Open an Existing Project**, then point to the `PocketGauge` directory.
3. Allow Gradle to sync dependencies.
4. Connect an Android device or start an emulator (API 24+).
5. Click **Run 'app'** (`Shift + F10`) to build and launch the application.

---

## 🧪 Running Tests

To run the unit test suite:
```bash
./gradlew test
```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
