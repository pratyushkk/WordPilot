# WordPilot — System-Wide Android Writing Assistant

[![Release](https://img.shields.io/badge/Release-v1.0-emerald?style=for-the-badge&logo=android)](https://github.com/pratyushkk/WordPilot/releases/tag/v1.0)
[![Website](https://img.shields.io/badge/Website-pratyushkk.github.io%2FWordPilot-teal?style=for-the-badge&logo=googlechrome)](https://pratyushkk.github.io/WordPilot/)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-blue?style=for-the-badge&logo=android)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Kotlin%20%7C%20Compose%20%7C%20MVVM-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Privacy](https://img.shields.io/badge/Privacy-Zero%20Telemetry%20%7C%20Banking%20Shield-green?style=for-the-badge&logo=shield)](https://github.com/pratyushkk/WordPilot)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange?style=for-the-badge)](LICENSE)

WordPilot is a production-grade, privacy-first Android writing assistant engineered to provide seamless, real-time grammar, spelling, and tone corrections across third-party applications. Featuring a non-intrusive floating overlay, sequential cloud API failover, and automated privacy shields, WordPilot elevates writing quality across any messaging, email, document, or social media app.

---

## 📥 Download Official Release

Download the official signed release APK directly from GitHub:

* **Direct APK Download**: [**WordPilot-v1.0.apk**](https://github.com/pratyushkk/WordPilot/releases/download/v1.0/WordPilot-v1.0.apk)
* **GitHub Release Page**: [**WordPilot v1.0 Release Assets & Changelog**](https://github.com/pratyushkk/WordPilot/releases/tag/v1.0)

> **Package Name**: `com.wordpilot.assistant`  
> **Version Name**: `1.0` (`versionCode = 1`)  
> **Minimum Android**: Android 8.0 (API 26)  
> **Target Android**: Android 15 / 16 (API 35)

---

## 🌟 Key Features

### 1. Cross-App Floating Assistant
* **Instant 1-Tap Replacements**: Tapping corrected word chips immediately applies the fix directly into the active text field with automated cursor synchronization.
* **Auto-Dock & 30% Edge-Tuck**: When collapsed, the assistant cleanly docks to the nearest screen edge and tucks 30% inward to avoid obstructing screen content.
* **Screen Boundary Awareness**: Expands dynamically within screen bounds with generous padding so suggestions never render off-screen.

### 2. Dual-Provider Automatic Failover
* **Sapling AI (Primary)**: High-speed neural contextual grammar, punctuation, and spelling suggestions.
* **LanguageTool (Automatic Fallback)**: Zero-configuration public endpoint (`https://api.languagetool.org`) active by default, with complete support for 100% private self-hosted Docker servers (`http://<ip>:8010`).
* **Resilient Failover**: If the primary provider encounters network timeout or rate limits, WordPilot instantly shifts to the fallback with zero user disruption.

### 3. Appearance & Layout Studio
* **AI Logo Customization**: 12 selectable icon styles (Sparkle Star, Magic Wand, Lightning Bolt, Writing Pen, Neural AI, Smart Assistant, Idea & Insight, Productivity Rocket, Premium Diamond, Star Emblem, Editorial Quill, Verified Check) with size presets (`18dp` to `36dp`) and a continuous slider.
* **Bubble Geometry Control**: Choose from Circle, Squircle, or Rounded Square shapes with customizable diameters (`24dp` to `64dp`).
* **Error Badge Formatting**: Display error counts directly on the bubble icon or with customizable letter identifiers (prefix or suffix) and font scaling.

### 4. Privacy & Banking Shield
* **Automated Banking Shield**: Detects 50+ banking, wallet, payment, and UPI applications (including Google Pay, PhonePe, Paytm, banking apps, and sensitive credential fields) and automatically pauses the assistant for privacy.
* **Automatic Resumption**: When switching back to any regular app (WhatsApp, Chrome, Notes, Gmail) or tapping standard text fields, WordPilot immediately unpauses and resumes assistance.
* **Quick Controls**: Includes an Android Quick Settings Tile and a 1-tap Home Screen Widget for rapid manual toggling.
* **Zero Telemetry**: No user analytics, no keystroke tracking, and no external tracking libraries.

### 5. Built-In Writing Editor & Dictionary
* **Standalone Editor**: Clean drafting canvas with live word and character counters, undo/redo stacks, and detailed rule feedback.
* **Personal Dictionary**: Add custom terminology, jargon, and proper names to prevent unwarranted spelling flags.
* **Document History**: Locally encrypted storage for past writing sessions.

---

## 🚀 Quick Setup Guide

### Step 1: Enable Accessibility Service
1. Install `WordPilot-v1.0.apk` on your device.
2. Open **WordPilot** and tap **"Floating Assistant (Cross-App)"** or **"Accessibility & Auto-Pause Guide"**.
3. Tap **"Open Accessibility Settings"**.
4. In system settings, navigate to **Downloaded apps** (or **Installed services**).
5. Select **WordPilot Assistant**, toggle it **ON**, and tap **"Allow"**.
6. *(Optional - MIUI / HyperOS / ColorOS)*: If prompted with *"Restricted setting"*, go to Phone Settings ➔ Apps ➔ WordPilot ➔ tap 3 dots (upper-right) ➔ tap *"Allow restricted settings"*, then re-enable accessibility.

### Step 2: Configure APIs
1. Open WordPilot and navigate to **API Providers & Keys**.
2. **Sapling AI (Primary)**:
   * Create a free account at [sapling.ai](https://sapling.ai) and copy your API Key from the dashboard.
   * Tap **"Paste Key"** in WordPilot and click **"Test Connection"**.
3. **LanguageTool (Fallback)**:
   * Pre-configured out of the box with the free public server (`https://api.languagetool.org`). No key or registration required!
   * For 100% private offline hosting, run Docker locally:
     ```bash
     docker run -d -p 8010:8010 erikvl87/languagetool
     ```
     Then set Server Base URL to `http://<your-server-ip>:8010`.

### Step 3: Prevent Background Service Termination
To ensure the floating bubble remains available when typing:
1. Go to **Settings** ➔ **Apps** ➔ **WordPilot** ➔ **Battery**.
2. Select **"Unrestricted"**.

---

## 🛠️ Technical Architecture

* **UI Toolkit**: Jetpack Compose (BOM 2024.09.00) + Material Design 3
* **Application Framework**: Android Jetpack (Lifecycle, ViewModel, Navigation, SavedState)
* **Local Persistence**: Room Database 2.6.1 + Jetpack DataStore Preferences
* **Concurrency**: Kotlin Coroutines & StateFlow / SharedFlow
* **Network Client**: OkHttp 4.12.0 with failover circuit breakers and exponential backoff
* **Build System**: Gradle 8.9, Android Gradle Plugin 8.5.2, Kotlin 1.9.24, KSP 1.9.24-1.0.20
* **Target Compatibility**: Android 8.0 (API 26) through Android 15 / 16 (API 35)

---

## 🏗️ Building from Source

To compile and package the release APK locally:

```bash
# Clone the repository
git clone https://github.com/pratyushkk/WordPilot.git
cd WordPilot

# Execute unit test suite
./gradlew testDebugUnitTest

# Assemble signed release APK
./gradlew assembleRelease
```

The compiled release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

WordPilot is open-source software licensed under the [Apache License, Version 2.0](LICENSE).
Built exclusively with permissive, open-source dependencies.
