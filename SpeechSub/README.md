# SpeechSub

**Automatic video captions with a timeline editor — for Android**

SpeechSub converts speech in your videos into editable captions. Import any video, get automatic speech-to-text transcription, edit captions on a timeline, style them, and export as SRT or text.

---

## Features

| Feature | Details |
|---|---|
| Video Import | Pick any local video — MP4, MKV, AVI, etc. |
| Speech-to-Text | Android's built-in recognizer — works offline |
| Language Support | English, Hindi, Hinglish (auto-detect) |
| Caption Timeline | Scrollable timeline with timestamps |
| Caption Editing | Tap any caption to edit text inline |
| Split & Merge | Split one caption into two, or merge consecutive ones |
| Caption Styling | Color picker, font picker, bold, italic, font size |
| Export | Copy to clipboard, export as SRT, export as plain text |
| Cloud Sync | Optional Firebase Firestore backup |
| Auth | Email/password login via Firebase Authentication |
| Dark Mode | Dark theme by default (Material Design 3) |
| Auto Updates | Version check on launch — notifies user of new releases |

---

## Project Structure

```
SpeechSub/
├── app/
│   ├── src/main/
│   │   ├── java/com/speechsub/
│   │   │   ├── data/
│   │   │   │   ├── firebase/       # AuthService, FirestoreService
│   │   │   │   ├── local/          # Room DB, DAOs, Entities, Service
│   │   │   │   └── repository/     # CaptionRepository (single truth)
│   │   │   ├── di/                 # Hilt dependency injection module
│   │   │   ├── ui/
│   │   │   │   ├── auth/           # Login & SignUp screens
│   │   │   │   ├── editor/         # Caption timeline editor
│   │   │   │   ├── export/         # Export & cloud sync
│   │   │   │   ├── home/           # Project list & video import
│   │   │   │   ├── navigation/     # NavGraph — all routes
│   │   │   │   ├── processing/     # Speech recognition progress
│   │   │   │   ├── settings/       # User preferences
│   │   │   │   ├── splash/         # Launch screen + version check
│   │   │   │   └── theme/          # Colors, typography, shapes
│   │   │   ├── MainActivity.kt
│   │   │   └── SpeechSubApp.kt
│   │   └── res/
│   │       ├── drawable/           # Vector icons
│   │       ├── font/               # Downloadable fonts (Google Fonts)
│   │       ├── mipmap-anydpi-v26/  # Adaptive launcher icons
│   │       ├── values/             # strings, colors, themes, font_certs
│   │       └── xml/                # backup_rules, data_extraction_rules
│   ├── build.gradle.kts
│   ├── google-services.json        # ⚠ Replace with your Firebase config!
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── .github/
│   └── workflows/
│       └── release.yml             # Auto-build & auto-release APK
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Quick Start — Build Locally

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 26–35 |
| Gradle | 8.x (wrapper included) |

### Step 1 — Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/SpeechSub.git
cd SpeechSub/SpeechSub
```

### Step 2 — Set up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project (name it "SpeechSub")
3. Click **Add app** → **Android**
4. Enter the package name: `com.speechsub`
5. Download `google-services.json`
6. Replace `app/google-services.json` with the downloaded file
7. In Firebase Console:
   - Enable **Authentication** → **Email/Password** sign-in method
   - Enable **Firestore Database** (production mode)
   - Enable **Storage** (optional, for future media uploads)

### Step 3 — Open in Android Studio

1. Open Android Studio
2. Click **Open** → select the `SpeechSub/` folder (the one with `settings.gradle.kts`)
3. Wait for Gradle sync to complete
4. Plug in your Android device (or start an emulator)
5. Click **Run ▶** — the app will build and install

### Step 4 — Build a Debug APK manually

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Build a Signed Release APK

### Create a keystore (do this once)

```bash
keytool -genkey -v \
  -keystore speechsub-release.keystore \
  -alias speechsub \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Save the keystore file and passwords somewhere safe.

### Build signed APK

```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=/path/to/speechsub-release.keystore \
  -Pandroid.injected.signing.store.password=YOUR_STORE_PASSWORD \
  -Pandroid.injected.signing.key.alias=speechsub \
  -Pandroid.injected.signing.key.password=YOUR_KEY_PASSWORD
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## 🐙 Push to GitHub & Set Up Auto-Release

### Step 1 — Create a GitHub repository

1. Go to [github.com/new](https://github.com/new)
2. Name it `SpeechSub`
3. Keep it private (recommended until ready to launch)
4. Don't initialize with README (you already have one)

### Step 2 — Push the code

```bash
cd /path/to/SpeechSub   # root of the project
git init
git add .
git commit -m "chore: initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/SpeechSub.git
git push -u origin main
```

### Step 3 — Add GitHub Secrets

Go to: **Your Repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | Paste the full content of your `google-services.json` file |
| `KEYSTORE_BASE64` | `base64 -i speechsub-release.keystore` (run this command, paste output) |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `speechsub` (or whatever alias you chose) |
| `KEY_PASSWORD` | Your key password |

### Step 4 — Create a release

Every time you push a version tag, GitHub Actions will:
1. Build a signed release APK
2. Create a GitHub Release
3. Attach the APK to the release automatically

```bash
git tag v1.0.0
git push origin v1.0.0
```

Users who have the app will see the "New version available" notification on next launch.

---

## 🔔 Version Check Setup (Firebase Remote Config)

The app checks for updates on launch. To make this work:

1. In Firebase Console → **Remote Config** → Add a parameter
2. Key: `latest_version`
3. Value: `1.0.1` (the newest version number)
4. Publish changes

The app compares this with `BuildConfig.VERSION_NAME` and shows an update dialog if a newer version is available.

> The update dialog links to your Google Play Store page or GitHub Releases.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Repository pattern |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| Video/Audio | AndroidX Media3 (ExoPlayer) |
| Speech-to-Text | Android SpeechRecognizer (built-in, offline) |
| Auth | Firebase Authentication |
| Cloud Storage | Firebase Firestore |
| Language Detection | Google ML Kit |
| Async | Kotlin Coroutines + StateFlow |
| Fonts | Google Fonts (downloadable fonts) |

---

## Architecture Overview

```
UI Layer (Compose)
    ↕ StateFlow
ViewModel Layer (Hilt)
    ↕ suspend functions
Repository Layer
    ↕                    ↕
Room (local DB)    Firestore (cloud)
```

- **Single Activity** — MainActivity hosts the entire NavGraph
- **Single Source of Truth** — CaptionRepository abstracts both Room and Firestore
- **Foreground Service** — SpeechProcessingService handles long audio processing without being killed
- **Modular** — each feature has its own package (home, editor, export, settings)

---

## Minimum Requirements

- **Android**: 8.0 (API 26) or higher
- **RAM**: 2 GB minimum
- **Storage**: 50 MB for app + space for exported files
- **Internet**: Required for Firebase auth and cloud sync (captions work offline)

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

MIT License — free to use, modify, and distribute.
