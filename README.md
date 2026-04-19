<p align="center">
  <img src="https://img.icons8.com/fluency/96/translation.png" width="80" alt="App Icon" />
</p>

<h1 align="center">On-Device Speech Translation</h1>

<p align="center">
  <strong>Translate speech &amp; text — entirely on your phone, no cloud required.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-informational" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-blue" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" />
</p>

---

## ✨ What It Does

A sleek, **dark-themed** Android app that runs a complete speech pipeline locally:

| Step | Engine | What Happens |
|------|--------|--------------|
| 🎙️ **Listen** | Vosk (on-device) | Speech → text in Hindi or Telugu |
| 🔄 **Translate** | ML Kit + Dictionary | Source text → English |
| 🔊 **Speak** | Android TTS (offline) | English translation read aloud |

> **Zero cloud calls at runtime.** Internet is only needed once — on first launch — to download model assets (~50 MB).

---

## 🖥️ App Modes

The app features a beautiful **dropdown selector** to switch between two modes:

### 🎙️ Speech Translation
Tap the mic, speak in Hindi or Telugu, and see the source transcript + English translation appear in real time. The translated text is automatically spoken aloud.

### 🔊 Text to Speech
Type any text (Hindi, Telugu, or English), then tap a button to hear it spoken in **English** or the **selected source language**.

---

## 🎨 Design

| Feature | Detail |
|---------|--------|
| **Theme** | Dark-only, OLED-friendly (`#0D0D1A` background) |
| **Palette** | Indigo-violet gradient accents (`#6C63FF → #9855F7`) |
| **Cards** | Rounded 20dp, accent-bar highlights (indigo / coral) |
| **Animations** | Pulse ring on mic, smooth page-fade transitions |
| **Components** | Material 3 — FAB, toggle group, exposed dropdown, tonal buttons |

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **Architecture** | MVVM — `MainActivity` + `MainViewModel` + `StateFlow` |
| **Speech-to-Text** | [Vosk Android 0.3.47](https://github.com/alphacep/vosk-api) |
| **Translation** | [ML Kit Translate 17.0.3](https://developers.google.com/ml-kit/language/translation) + local dictionary fallback |
| **Text-to-Speech** | Android `TextToSpeech` (offline voices) |
| **UI** | XML + ViewBinding + Material 3 + Lottie |
| **Networking** | OkHttp 4.12 (model download only) |
| **Async** | Kotlin Coroutines + StateFlow |

---

## 📦 Model Assets

### STT Models (Vosk)

| Language | Model | License |
|----------|-------|---------|
| Hindi | [vosk-model-small-hi-0.22](https://github.com/Sarthak-Jaiswal4/Speech-translation/releases/download/v1.0/vosk-model-small-hi-0.22.zip) | Apache-2.0 |
| Telugu | [vosk-model-small-te-0.42](https://github.com/Sarthak-Jaiswal4/Speech-translation/releases/download/v1.0/vosk-model-small-te-0.42.zip) | Apache-2.0 |

### Translation Model (ML Kit)
| Language Pair | Provider | Download | License |
|---|---|---|---|
| Hindi → English | Google ML Kit Translate 17.0.3 | Auto-downloaded on first use over WiFi | Apache-2.0 |
| Telugu → English | Google ML Kit Translate 17.0.3 | Auto-downloaded on first use over WiFi | Apache-2.0 |

### Translation Dictionaries

- Hindi → English dictionary (GitHub raw URL, configurable in `ModelCatalog.kt`)
- Telugu → English dictionary (GitHub raw URL, configurable in `ModelCatalog.kt`)

> 💡 You can swap dictionary URLs with Hugging Face / GitHub transformer checkpoints and keep the same download-manager contract.

---

## 🚀 Getting Started

### Download APK
Download the latest APK from 
[GitHub Releases](https://github.com/Sarthak-Jaiswal4/Speech-translation/releases)

### Prerequisites

- **Android Studio** Giraffe or newer
- **JDK 17**
- Physical device or emulator running **API 26+**

### Installation

```bash
# 1 — Clone the repo
git clone https://github.com/Sarthak-Jaiswal4/Speech-translation.git
cd Speech-translation

# 2 — Open in Android Studio → let Gradle sync

# 3 — Run on device
./gradlew assembleDebug
# or just hit ▶ Run in Android Studio
```

### First Launch

1. **Grant microphone permission** when prompted.
2. Select a source language (**Hindi** or **Telugu**).
3. Wait for the one-time model download to complete (progress bar shown).
4. You're ready — the app works **fully offline** from now on.

---

## 📖 Usage

### Speech Translation Mode

1. Choose **🎙️ Speech Translation** from the dropdown.
2. Select **Hindi** or **Telugu**.
3. Tap the **mic button** — speak clearly.
4. Watch the source transcript appear, followed by the English translation.
5. The English text is spoken aloud automatically.

### Text to Speech Mode

1. Choose **🔊 Text to Speech** from the dropdown.
2. Type text in any language (Hindi, Telugu, or English).
3. Tap **Speak English** to translate + speak, or **Speak Source** to hear it in the selected language.

---

## 🔧 How Models Are Downloaded

```
ModelDownloadManager
  ├── Downloads ZIP files to filesDir/models/
  ├── Extracts archives on-device
  └── Reports progress via StateFlow → UI
```

- Download is triggered by `MainViewModel.initializeSelectedLanguage()`
- Progress is shown with a Lottie animation + progress bar
- After download, models are cached locally — no re-download on subsequent launches

---

## 📂 Project Structure

```
app/src/main/
├── java/com/assignment/ondevicetranslation/
│   ├── ui/
│   │   ├── MainActivity.kt          # Activity + UI logic
│   │   ├── MainViewModel.kt         # MVVM ViewModel
│   │   └── MainUiState.kt           # UI state data class
│   └── data/
│       ├── download/                 # Model download manager
│       ├── model/                    # ModelCatalog, SourceLanguage
│       ├── stt/                      # Vosk speech recognizer
│       ├── translation/              # Dictionary + ML Kit translators
│       └── tts/                      # Android TTS engine wrapper
├── res/
│   ├── layout/activity_main.xml      # Main layout (dark, two-page)
│   ├── drawable/                     # Gradients, icons, accent bars
│   ├── values/colors.xml             # Dark palette
│   ├── values/themes.xml             # Material 3 dark theme
│   └── values/strings.xml            # All user-facing strings
└── AndroidManifest.xml
```

---

## ⚠️ Limitations

- ML Kit translation requires a one-time WiFi download from Google servers 
  per language pair. After that it works fully offline.
- Dictionary fallback covers only ~30 words per language and is used when 
  ML Kit model has not yet been downloaded.
- Translation layer uses a **dictionary-based** approach (not full neural MT).
- STT accuracy depends on microphone quality and speaker accent.
- Initial model download can be slow on weak connections.
- TTS requires an offline English voice package to be installed on the device.

---

## 🔮 Future Improvements

- [ ] Replace dictionary translator with on-device neural MT (ONNX / TFLite)
- [ ] Add voice activity detection for real-time chunking
- [ ] Implement model integrity checks (SHA-256) and resumable downloads
- [ ] Introduce dependency injection (Hilt / Koin)
- [ ] Add waveform visualizer, streaming confidence scores, and conversation history

---

## ✅ Testing Checklist

| # | Test | Expected Result |
|---|------|-----------------|
| 1 | Install APK / run debug build | App launches without crash |
| 2 | Grant microphone permission | Permission granted, models begin download |
| 3 | Speak a Hindi phrase | Transcript + English translation + TTS playback |
| 4 | Speak a Telugu phrase | Transcript + English translation + TTS playback |
| 5 | Switch to Text to Speech mode | Dropdown switches, TTS input card appears |
| 6 | Type text → tap Speak English | Text translated and spoken in English |
| 7 | Toggle airplane mode → retry | App works fully offline with cached models |

---

<p align="center">
  Built with ❤️ using Kotlin, Material 3, and on-device AI
</p>
