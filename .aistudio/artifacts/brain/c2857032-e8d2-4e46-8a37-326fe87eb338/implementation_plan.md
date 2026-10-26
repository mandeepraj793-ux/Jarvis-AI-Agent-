# JARVIS AI — Lightweight Intelligent Android Agent

A professional, secure, and voice-controlled Android AI assistant built natively with Kotlin and Jetpack Compose. JARVIS AI features natural language comprehension in English, Hindi, and Hinglish, real-time speech-to-text, Text-to-Speech synthesis, Gemini 3.8 Flash reasoning with structured tool actions, an allowlisted Android action executor, Room local persistence, and strict security controls.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural parameters have been established based on your preferences:

- **Confirmed Decision 1: Voice Activation**: Tap-to-talk microphone trigger with a central animated reactive audio visualizer and touch-to-cancel controls (no background battery draining or secret recording).
- **Confirmed Decision 2: AI Reasoning Profile**: Primary reasoning and structured tool generation powered by `gemini-3.8-flash` via secure REST API with fallback to offline predefined commands when network is unavailable.
- **Confirmed Decision 3: Security & Safety Policy**: Mandatory confirmation dialogs for consequential/sensitive actions (e.g. creating calendar events, setting alarms, toggling system settings, sending communications), while read-only or safe navigation queries execute directly.
- **Confirmed Decision 4: Multilingual Voice Support**: Native speech recognition tuned for English (`en-US`, `en-IN`) and Hindi (`hi-IN`), with robust natural language understanding of conversational Hinglish.

---

### 1. Overview & Core Concept

**JARVIS AI** is an intelligent on-device personal assistant that bridges cloud reasoning and real Android system execution without relying on simulated actions or fake mocks:

- **Voice & Typed Interaction**: SpeechRecognizer with live partial results display, animated frequency ring, and Android TextToSpeech audio feedback.
- **Structured Action Brain**: Translates natural human requests into typed, schema-validated device intents (launching apps, opening system settings, searching the web, scheduling reminders/calendar events, taking notes, reading saved info).
- **Honest Capability Boundaries**: Clear system prompts and UI badges indicating what Android permissions allow vs what Android sandboxing restricts (e.g. inability to bypass lock screens or execute root commands).
- **Privacy & Security First**: Device-local Room database with instant one-tap data purging, strict input sanitization, and explicit user-granted permissions.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Voice Query Flow**:
   User taps the central glowing mic button $\rightarrow$ Voice visualizer pulses with audio amplitude $\rightarrow$ Recognized speech displays live in English/Hindi $\rightarrow$ Query sent to Gemini 3.8 Flash $\rightarrow$ Assistant speaks the response via TextToSpeech and presents action cards.
2. **Action Execution Flow**:
   Assistant parses intent (e.g., *"Open YouTube and search lo-fi music"* or *"Kal subah 7 baje ka alarm lagao"*) $\rightarrow$ If sensitive (alarm/calendar/settings), displays a distinct Confirmation Sheet with parameters $\rightarrow$ User confirms $\rightarrow$ Intent executes via Android system API $\rightarrow$ Success/failure recorded and announced.
3. **Offline Fallback Flow**:
   Network disconnected or offline mode enabled $\rightarrow$ Pre-trained rule engine executes local app launches, flashlight toggle, offline notes retrieval, and tells the user with TTS that cloud reasoning is paused.
4. **Settings & Privacy Dashboard**:
   User accesses settings to customize voice pitch/speed, preferred language (English / Hindi / Hinglish), manage permissions, inspect logged commands, or clear all conversation history from SQLite.

#### Visual Identity & Theme

- **Aesthetic Direction**: Futuristic Stark-Tech cockpit, sleek, clean, and utilitarian without heavy 3D rendering overhead.
- **Color Palette**:
  - Background: Deep Void Navy (`#0A0F1D`) and Midnight Slate (`#0F172A`)
  - Accent / Arc Glow: Cyber Cyan (`#00E5FF`) and Electric Azure (`#0284C7`)
  - Surface Containers: Glassmorphic Navy (`#1E293B` at 85% opacity with subtle 1dp border `#334155`)
  - Status Indicators: Emerald Green (`#10B981`) for listening/ready, Amber (`#F59E0B`) for action confirmation, Crimson (`#EF4444`) for errors.
- **Typography & Rhythm**:
  - Display / HUD Headings: High-contrast geometric sans with tracked letter spacing
  - Dialogue / Chat Bubbles: Clean readable body font at accessible `sp` scales
  - Touch targets strictly $\ge 48\text{dp}$ with haptic vibration feedback.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native SpeechRecognizer vs Third-Party STT**
  - *Chosen Approach*: Android's built-in `SpeechRecognizer` with `RecognizerIntent.LANGUAGE_MODEL_FREE_FORM` supporting `en-IN`, `hi-IN`, and multi-lingual hints.
  - *Why*: Zero APK bloat (0MB added), zero cloud audio latency/cost for transcription, and full on-device privacy where supported by Google Speech Services.
- **Decision 2: Structured JSON Action Contract with Gemini 3.8 Flash**
  - *Chosen Approach*: Direct REST client using Kotlinx Serialization and Gemini `responseMimeType = "application/json"` with schema enforcement.
  - *Why*: Guarantees predictable typed payloads (`action`, `target`, `arguments`, `explanation`, `requires_confirmation`) without regex guesswork.
- **Decision 3: Room Database for Local History**
  - *Chosen Approach*: Room SQLite database storing chat messages, action logs, and user notes.
  - *Why*: Instant local retrieval, zero network dependency for history, and complete user ownership of their personal logs with a 1-tap delete button.
- **Decision 4: Allowlisted System Intent Executor**
  - *Chosen Approach*: Isolated `ActionExecutor` mapping approved action schemas to explicit Android Intents (`ACTION_MAIN`, `ACTION_VIEW`, `ACTION_SET_ALARM`, `ACTION_INSERT`, Settings intents). Arbitrary reflection, shell commands, and untrusted component names are strictly blocked.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          JARVIS Android App                            │
├────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer (Jetpack Compose + Material 3)                     │
│  ┌───────────────────────┐  ┌─────────────────┐  ┌──────────────────┐  │
│  │ AssistantHomeScreen   │  │ VoiceVisualizer │  │ ActionConfirmDlg │  │
│  │ (Chat Thread & HUD)   │  │ (Pulse & Waves) │  │ (Security Gate)  │  │
│  └───────────▲───────────┘  └────────▲────────┘  └────────▲─────────┘  │
│              │                       │                    │            │
│  ┌───────────┴───────────────────────┴────────────────────┴─────────┐  │
│  │                    JarvisViewModel                               │  │
│  │ (StateFlow: JarvisUiState, SpeechState, ActionQueue, History)     │  │
│  └───────────▲───────────────────────▲────────────────────▲─────────┘  │
├──────────────┼───────────────────────┼────────────────────┼────────────┤
│  Domain & Core Modules               │                    │            │
│  ┌───────────▼───────────┐  ┌────────▼────────┐  ┌────────▼─────────┐  │
│  │   VoiceInputManager   │  │ TextToSpeechMgr │  │ ActionExecutor   │  │
│  │  (SpeechRecognizer)   │  │ (Android TTS)   │  │ (Intents/Allow)  │  │
│  └───────────────────────┘  └─────────────────┘  └────────▲─────────┘  │
├───────────────────────────────────────────────────────────┼────────────┤
│  Data & AI Layer                                          │            │
│  ┌───────────────────────┐                        ┌───────▼─────────┐  │
│  │ GeminiAssistantClient │                        │ JarvisDatabase  │  │
│  │ (REST / 3.8 Flash)    │                        │ (Room SQLite)   │  │
│  └───────────────────────┘                        │ Messages & Notes│  │
│                                                   └─────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Supported Allowlisted Device Actions

1. `OPEN_APP`: Resolves application launch intents using `PackageManager.getLaunchIntentForPackage` or known packages (YouTube, WhatsApp, Maps, Chrome, Camera, Calculator, Spotify, etc.).
2. `SEARCH_WEB`: Launches `Intent.ACTION_WEB_SEARCH` or view URL in the default browser.
3. `SET_ALARM_OR_TIMER`: Launches `AlarmClock.ACTION_SET_ALARM` or `ACTION_SET_TIMER` with time/label.
4. `CREATE_NOTE`: Saves note directly to Room `NoteEntity` and confirms to user.
5. `OPEN_SETTINGS`: Opens specific settings screens (`ACTION_SETTINGS`, `ACTION_WIFI_SETTINGS`, `ACTION_BLUETOOTH_SETTINGS`, `ACTION_APPLICATION_DETAILS_SETTINGS`).
6. `MAKE_CALL_PROMPT`: Prepares dialer intent (`Intent.ACTION_DIAL`) with validated phone number (safe, never initiates calls automatically without dialer review).
7. `SEND_MESSAGE_PROMPT`: Opens SMS/Share intent with text.
8. `SPEAK_TEXT`: TTS vocal response.

---

### Implementation Phases

1. **Phase 1: Project Setup & Dependencies**
   - Update `metadata.json` and `res/values/strings.xml` to `JARVIS AI`.
   - Uncomment `GEMINI_API_KEY` in `.env.example`.
   - Configure Gradle dependencies (Room, Retrofit, Kotlinx Serialization, Accompanist permissions).
   - Add permissions in `AndroidManifest.xml` (`RECORD_AUDIO`, `INTERNET`, `VIBRATE`, etc.).
2. **Phase 2: Local Persistence & Room Data Layer**
   - Implement `JarvisDatabase`, `MessageEntity`, `NoteEntity`, `ActionAuditEntity`.
   - Implement DAOs and `JarvisRepository`.
3. **Phase 3: Voice Engine & Audio Synthesis**
   - Implement `VoiceInputManager` wrapping `SpeechRecognizer` with error handling and audio amplitude listeners.
   - Implement `TextToSpeechManager` with pitch/speed controls and multilingual voice selection.
4. **Phase 4: Gemini AI Service & JSON Action Parser**
   - Implement `GeminiApiService` targeting `gemini-3.8-flash`.
   - Implement prompt engineering with system instructions enforcing strict structured action output and Hindi/Hinglish/English support.
   - Implement schema validation and defensive error handling.
5. **Phase 5: Action Executor & Security Gate**
   - Implement `ActionExecutor` with allowlist checking, package validation, and execution verification.
   - Implement confirmation dialogs for sensitive device actions.
6. **Phase 6: UI Design & Compose Screens**
   - Create futuristic dark-navy HUD theme in `Theme.kt` and `Color.kt`.
   - Build reactive `ArcReactorVisualizer` for mic listening/thinking/speaking states.
   - Build chat history feed, quick command chips, action preview cards, and settings drawer.
7. **Phase 7: Verification & Testing**
   - Implement unit tests for command schema parsing, action allowlisting, and security guards.
   - Run `compile_applet` and verify zero compilation errors.
