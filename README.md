# SpamBlock 🛡️

An ultra-lightweight, battery-efficient Android application that automatically blocks unknown phone callers and private/hidden numbers in the background with **zero persistent hardware or battery drain**.

---

## ⚡ Why SpamBlock Uses Zero Processing Power

Traditional spam blocker apps run persistent foreground services, poll remote servers, and keep wake-locks active, causing severe battery and RAM consumption.

**SpamBlock** is built on Android's native **`CallScreeningService`** API:
- **Zero Idle Overhead**: Consumes **0% CPU** and **0 MB RAM** while idle. No background processes or worker threads run when calls are not active.
- **Event-Driven OS Telecom Hook**: The Android Telephony system invokes the screening service on an isolated IPC thread only when an incoming call starts ringing.
- **Fast Indexed B-Tree Lookup (<5ms)**: Queries Android's native `ContactsContract.PhoneLookup` SQLite database to identify unknown callers.
- **Silent Drop**: Rejects/hangs up on unknown numbers before the phone screen or ringer even wakes up.
- **100% Offline & Private**: Zero internet permissions requested, zero tracking, zero data uploaded.

---

## ✨ Features

- 🛡️ **Block Non-Contacts**: Automatically drop any call from numbers not saved in your contacts book.
- 🚫 **Block Private / Hidden Numbers**: Intercept anonymous, restricted, or private caller IDs.
- ⚡ **Immediate Disconnect or Silent Mute**: Choose between hanging up immediately or silently muting the ringer.
- 📋 **Blocked Calls History**: Keep track of intercepted calls, timestamps, and reasons with an option to clear anytime.
- ⚪ **Custom Allowed Whitelist**: Whitelist essential numbers (e.g. delivery drivers, clinics, banks) so they always ring even if not saved in your contacts.
- 🎨 **Modern Jetpack Compose UI**: Clean Material 3 design with dark mode and dynamic colors.

---

## 📱 Requirements

- Android 10 (API Level 29) or higher.

---

## 🚀 Getting Started

### 1. Build and Install via ADB
```bash
# Clone the repository
git clone https://github.com/dxlmartin00/SpamBlock.git
cd SpamBlock

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. One-Time Setup in the App
1. Tap **"Set as Caller ID & Spam App"** and confirm SpamBlock as your default screening app.
2. Tap **"Grant Contacts Access"** so SpamBlock can check if incoming callers are in your address book.
