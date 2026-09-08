# SpamBlock

SpamBlock is an ultra-lightweight, hardware-efficient Android application that automatically screens and blocks unknown callers, telemarketers, and private numbers in the background with zero persistent battery, CPU, or memory consumption.

---

## Technical Overview

Traditional spam-blocking applications run persistent background services, maintain wake-locks, and periodically query remote servers, leading to significant battery drain and memory overhead.

SpamBlock operates strictly on Android's native Telecom framework:

- **Zero Idle Overhead**: The application process does not run in the background when the phone is idle. It consumes 0% CPU, 0 MB RAM, and holds no wake-locks.
- **Event-Driven Telecom Integration**: Uses Android's `CallScreeningService` API. The operating system invokes the screening service on an isolated IPC thread only at the moment an incoming call arrives.
- **Sub-5ms Execution**: Queries Android's native `ContactsContract.PhoneLookup` SQLite database (indexed B-Tree maintained by the operating system) to identify unknown numbers before the screen or ringer can turn on.
- **Optimized Footprint**: Built with R8 minification and resource shrinking, resulting in a compact 3.39 MB release APK.
- **Privacy-First**: No remote phone tracking, no contact book uploads, and no analytics SDKs.

---

## Core Features

### Independent Dual-SIM Slot Protection
- Configure screening policies separately for SIM 1 and SIM 2 (for example, leaving a personal SIM open while strictly blocking unknown callers on a business SIM).
- Uses a multi-layered telephony resolution pipeline (`TelephonyManager.getSubscriptionId`, `SubscriptionManager`, ICCID matching, and Telecom account inspection) to reliably identify the incoming physical SIM slot across modern Android OEM platforms (including Realme UI, ColorOS, One UI, and stock Android).

### Repeated Callers Emergency Bypass
- Prevents missing urgent calls from delivery drivers, clinics, or unexpected family emergencies.
- Automatically permits an unknown caller to ring through if they call repeatedly (configurable to 2 or 3 attempts) within a 5-minute window.
- Powered by a composite SQLite index `(phone_number, timestamp)` for instant historical lookup without persistent workers.

### Granular Screening Rules
- **Block Non-Contacts**: Automatically drop any call from numbers not saved in your address book.
- **Block Private / Hidden Numbers**: Drop callers with restricted, anonymous, or hidden caller IDs.
- **Immediate Disconnect vs. Silent Mute**: Choose between terminating the call immediately (`rejectCall`) or silently suppressing the ringer without hanging up.
- **Silent Notifications**: Receive a discreet, non-intrusive notification when a call is intercepted, tagged with the respective SIM slot (`[SIM 1]` or `[SIM 2]`).

### Allowed Whitelist
- Maintain a local whitelist for custom numbers that should always ring through, even if not saved in your device's contacts.

### In-App Update Notifications
- Checks GitHub's public releases API on application launch using a lightweight coroutine.
- Displays an update notification card with an "Update Now" action that directs the user to the latest release page.

### Minimalist User Interface
- Modern, distraction-free interface built with Jetpack Compose and Material 3.
- Cohesive typography and iconography adhering to Google's official Outlined icon set and monospace alignment for phone numbers and timestamps.

---

## System Requirements

- **Operating System**: Android 10 (API Level 29) or higher.
- **Permissions**:
  - `READ_CONTACTS`: Required to determine whether an incoming caller is in your address book.
  - `READ_PHONE_STATE`: Required for multi-SIM discovery and routing incoming calls to their physical SIM slot.
  - `POST_NOTIFICATIONS`: Optional; used to deliver quiet notifications when a call is intercepted.
  - `INTERNET`: Optional; used exclusively to check GitHub Releases for app updates on app launch.

---

## Installation

### Option 1: Download Release APK (Recommended)
1. Navigate to the **Releases** section of this repository:
   https://github.com/dxlmartin00/SpamBlock/releases
2. Download the latest `app-release.apk` directly to your Android device.
3. Open the downloaded file and install it (enable "Install unknown apps" for your browser if prompted).

### Option 2: Install via ADB
```bash
# Download or locate app-release.apk, then install via ADB:
adb install -r app-release.apk
```

### Option 3: Build From Source
```bash
# Clone the repository
git clone https://github.com/dxlmartin00/SpamBlock.git
cd SpamBlock

# Build the signed release APK
./gradlew assembleRelease

# Install onto your connected Android device
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Initial Setup

1. Launch **SpamBlock** on your device.
2. Tap **"Set as Caller ID & Spam App"** and select SpamBlock in the system dialog. This registers the application with Android's Telecom subsystem.
3. Tap **"Grant Contacts Access"** and allow access to Contacts.
4. If using a dual-SIM device, grant Phone State access to enable per-SIM slot controls.
5. Customize your screening rules and SIM card slot preferences on the Protection dashboard.
