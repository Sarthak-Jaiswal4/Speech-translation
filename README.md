# On-Device Speech Translation Android App

## Description
This project is an Android app (Kotlin + MVVM) that performs a local speech pipeline:

1. **Speech-to-Text (STT)** using Vosk on-device models  
2. **Hindi/Telugu to English translation** using an on-device dictionary translator layer  
3. **Text-to-Speech (TTS)** using Android offline TTS engine

All runtime inference happens on-device. The app only needs internet on first launch to download model assets.

## Features
- Offline-first architecture after initial setup
- Hindi and Telugu input language selection
- One-tap microphone start/stop
- Live source transcript and translated English output
- Spoken English output via local TTS
- First-launch model download with progress status
- MVVM-based code structure with clear separation of concerns

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM (`MainActivity` + `MainViewModel`)
- **STT:** [Vosk Android](https://github.com/alphacep/vosk-api)
- **Networking:** OkHttp
- **Async:** Kotlin Coroutines + StateFlow
- **UI:** XML + ViewBinding
- **TTS:** Android `TextToSpeech` (offline voices)

## Model Details (Hugging Face/GitHub)
### STT models
- Hindi Vosk model: [vosk-model-small-hi-0.22](https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip)  
  License: Apache-2.0
- Telugu Vosk model: [vosk-model-small-te-0.42](https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip)  
  License: Apache-2.0

### Translation assets
- Hindi-English dictionary source (GitHub raw URL, configurable in `ModelCatalog.kt`)
- Telugu-English dictionary source (GitHub raw URL, configurable in `ModelCatalog.kt`)

> You can replace dictionary URLs with Hugging Face/GitHub transformer checkpoints and keep the same download manager contract.

## Installation Steps
1. Open project in Android Studio (Giraffe+ recommended).
2. Let Gradle sync and download dependencies.
3. Connect Android device/emulator (API 26+).
4. Run the app.

## Usage Instructions
1. Grant microphone permission.
2. Select source language (Hindi or Telugu).
3. Wait until model download reaches 100% (first launch only).
4. Tap **Start** and speak.
5. Verify:
   - Source transcript appears
   - English translation appears
   - English TTS output is played
6. Disable network and test again; app continues working offline with downloaded assets.

## How Models Are Downloaded
- `ModelDownloadManager` downloads each model URL into `filesDir/models`.
- ZIP files are extracted on-device.
- Model loading is triggered by `MainViewModel.initializeSelectedLanguage()`.
- Download progress is surfaced to UI via `StateFlow`.

## How to Run
- From Android Studio: click **Run app**.
- If you prefer CLI, generate Gradle wrapper once and run:
  - `./gradlew assembleDebug`

## Limitations
- Translation layer is dictionary-based in this baseline implementation (not full neural MT).
- STT quality depends on mic quality and speaker accent.
- Initial model download can be large and slow on weak networks.
- TTS depends on installed offline English voice package.

## Future Improvements
- Replace dictionary translator with on-device neural MT (ONNX/TFLite from Hugging Face).
- Add voice activity detection for better real-time chunking.
- Add model integrity checks (SHA256) and resumable downloads.
- Introduce dependency injection (Hilt/Koin) and repository abstractions.
- Improve UI with waveform, streaming confidence, and conversation history.

## Testing Checklist
- Install APK / run debug build
- Allow microphone permission
- Speak Hindi phrase; check transcript/translation/TTS
- Speak Telugu phrase; check transcript/translation/TTS
- Toggle airplane mode after first launch and re-test full flow offline
