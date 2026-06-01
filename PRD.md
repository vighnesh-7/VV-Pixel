# Product Requirement Document (PRD) — VV Pixel Utility App

## 1. Product Overview
**VV Pixel** is a utility application designed specifically for Pixel 10 (and compatible stock Android devices) to address native user experience gaps, improve accessibility, and provide quick hardware/system-level shortcuts. The application focuses on high-efficiency battery consumption, compliance with material system styling (Material You), and offline-only operations (no Internet access required).

## 2. Problem Statement & Functional Requirements

### 2.1 Double Tap to Lock & Lock Widget (2x1)
*   **The Problem:** Quickly locking the phone from the homescreen without pressing the physical power button is a desirable quality-of-life feature. On stock Pixel devices, there is no native gesture for empty space double-tap to lock without using third-party launchers.
*   **The Solution:**
    1.  **Accessibility Service Integration:** Implement an `AccessibilityService` that can trigger screen lock via `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`. This ensures that biometrics (fingerprint/face unlock) continue to function perfectly (unlike old DeviceAdmin options which force PIN entry).
    2.  **2x1 Lock Widget:** A beautiful, responsive home screen widget of size 2x1. It features a prominent "Lock Screen" action button, styled in sync with Android's Material You colors.
    3.  **1x1 Double-Tap Transparent Invisible Zone Widget:** A tiny widget users can place on empty homescreen slots. Tapping or double-tapping it instantly triggers the lock screen service, simulating "double tap empty space to lock".
    4.  **Floating Lock Assist Bubble:** An optional on-screen floating bubble that can be double-tapped from any screen to lock the device immediately.

### 2.2 Homescreen Calculator Widget
*   **The Problem:** Standard calculators require launching a separate app. Having a widget lets users perform quick math, but standard widget state vanishes on update or device restart.
*   **The Solution:**
    1.  **Calculations on Widget:** A beautiful Material You 4x3 (or responsive) interactive App Widget displaying digits, basic operands (`+`, `-`, `*`, `/`), clear (`C`), and compute (`=`).
    2.  **Persistent Registry:** Every calculation state (the expression currently entered and the last calculated result) is written to encrypted/local `SharedPreferences` or database. When the widget redraws or the device restarts, the state remains intact.

### 2.3 Adaptive Brightness Toggle in QS Panel
*   **The Problem:** In Stock Android/Pixel, modifying the native system Quick Settings brightness slider is restricted to system applications. Toggle adaptive brightness is buried or requires standard settings navigation.
*   **The Solution:**
    1.  **Adaptive Brightness QS Tile (QsTileService):** Register a custom Quick Settings Tile `Adaptive Brightness` which toggles `Settings.System.SCREEN_BRIGHTNESS_MODE` directly using `WRITE_SETTINGS` permission.
    2.  **Interactive Tile UI:** The tile displays active/inactive state aligning automatically with system dynamic themes, allowing adaptive brightness toggling with a single tap in the QS expanded or collapsed view.

### 2.4 Single-Swipe Brightness Bar Access
*   **The Problem:** Accessing the brightness bar requires swiping down the Quick Settings panel twice on stock Pixel devices.
*   **The Solution:**
    1.  **Persistent One-Swipe Notification Control:** Implement a custom, system-styled, low-priority persistent control notification showing a real-time brightness slider or direct action buttons ("-10%", "+10%", "Max", "Auto") inside the notification drawer.
    2.  This notification is visible on a single swipe down from the status bar, granting instant single-swipe access to brightness controls.

### 2.5 Quick Settings Volume Slider
*   **The Problem:** Pixels have physical keys for volume, but lack a quick drag slider in the status bar/QS section.
*   **The Solution:**
    1.  **Volume Slider QS Tile:** Create a custom Quick Settings Tile "Volume Slider".
    2.  **Floating Overlay / Custom Dialog:** Clicking this tile immediately launches a beautiful system-styled floating panel containing a volume slider synced in real-time with physical volume keys.
    3.  The slider utilizes `AudioManager` to modify volumes and implements a broadcast receiver/observer to sync back immediately when physical buttons are clicked.

### 2.6 Shake-to-Torch Gesture (Motorola Style)
*   **The Problem:** Quick toggle for flashlight is useful, but tapping tiles is slow.
*   **The Solution:**
    1.  **Gesture Detection Service:** Register a lightweight background service monitoring `Sensor.TYPE_ACCELEROMETER`.
    2.  **3-Step Chop Algorithm (Right-Left-Right):** To prevent accidental triggers in the pocket, the algorithm requires a specific lateral rapid oscillation:
        *   Phase 1: Accel in +X exceeds threshold.
        *   Phase 2: Accel in -X exceeds negative threshold within 300ms.
        *   Phase 3: Accel in +X exceeds threshold again within 300ms.
    3.  **Torch Control:** Triggers `CameraManager` torch mode on/off on successful gesture match.
    4.  **Battery Optimization:**
        *   **Mode 1 (Eco-Friendly):** Screen is off -> sensor is unregistered (Zero battery idle drain). Gesture works only when screen is on.
        *   **Mode 2 (Always-On):** Runs as a Foreground Service keeping a wake lock. Unregisters sensor if the device is static (using coarse step detection/gravity sensor) to preserve battery.
        *   Main toggles allow the user to select their desired behavior.

---

## 3. Architecture & Permissions

### Permissions Requested
The application adheres strictly to the principle of minimal permissions. No internet access is declared:
*   `android.permission.WRITE_SETTINGS` - To modify adaptive and manual screen brightness.
*   `android.permission.CAMERA` - Standard permission to control flashlight on/off without opening camera preview.
*   `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` - To maintain the shake-to-torch active background service cleanly on modern Android 14+ devices.
*   `android.permission.SYSTEM_ALERT_WINDOW` - To draw the custom volume overlay directly on top when clicking the volume QS tile.
*   `android.permission.BIND_ACCESSIBILITY_SERVICE` - To allow the application to execute device locks natively.

---

## 4. Work Plan and Implementation Path

1.  **Project Metadata and Setup:** Update `metadata.json` and `strings.xml` to rename the application to "VV Pixel".
2.  **Accessibility Service (`VVPixelAccessibilityService`):** Write code to handle global lock screen triggers.
3.  **Double-Tap Widget & lock widget (2x1):** Write Kotlin providers `DoubleTapWidgetProvider` and `LockWidget2x1Provider`.
4.  **Persistent Calculator Widget:** Write `CalculatorWidgetProvider` with math parsing engine and SharedPreferences configuration.
5.  **Service & Torch Shake Management:** Write `ShakeTorchService` focusing on low-energy accelerometer patterns.
6.  **Quick Settings Tiles Service:**
    *   `AdaptiveBrightnessTile`: Handle toggling values in system table.
    *   `VolumeSliderTile`: Launch overlay activity or dynamic dialog with volume control block.
7.  **Main Application Interface (Jetpack Compose):** A beautiful console providing full visual toggle buttons to enable/disable each helper dynamically with step-by-step guides for custom widget placements.
