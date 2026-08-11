# BLE Signal Analyzer

BLE Signal Analyzer is a local-only Android utility for analyzing and comparing Bluetooth Low
Energy signal strength. It scans nearby BLE advertisements, shows relative RSSI changes in real
time, and displays advertised device, manufacturer, service UUID, Tx Power, and connectable
information when those fields are available.

This repository contains the Android project and public release documentation for BLE Signal
Analyzer.

RSSI is affected by hardware and the surrounding environment. The app does not calculate exact
distance or exact device location.

## Public website

Website: https://changhuliu.github.io/BLE-Signal-Analyzer/

- [Privacy Policy](https://changhuliu.github.io/BLE-Signal-Analyzer/privacy.html)
- [Support](https://changhuliu.github.io/BLE-Signal-Analyzer/support.html)
- [Terms of Use](https://changhuliu.github.io/BLE-Signal-Analyzer/terms.html)

Support contact: artbyte@126.com

## Architecture

- Kotlin, Jetpack Compose, and Material 3
- Single `MainActivity`
- `MainViewModel` with `StateFlow`
- `AndroidBleScanner` around the official Android `BluetoothLeScanner` API
- Preferences DataStore for local settings
- AndroidX AppCompat per-app language support for 13 app languages
- Pure Kotlin helpers for filtering, sorting, RSSI smoothing, trend calculation, statistics, and
  proximity-alert evaluation

BLE devices and RSSI samples live only in memory. There is no Room database, backend, account,
cloud sync, advertising, analytics, or network communication.

## V2 features

- Compare two BLE device signals in real time using one shared scan stream
- Signal Stability Score in Signal Tracker and device comparison
- Advertisement Inspector to inspect BLE advertisement data and raw bytes
- BLE Environment Analyzer to summarize nearby BLE device activity and signal distribution
- User-triggered CSV export for current Signal Tracker, comparison, and environment sessions

The Signal Stability Score is an app-defined relative metric based on recent RSSI variation,
signal range, sudden changes, and signal continuity. It is not an official Bluetooth standard
measurement. Comparison and stability samples remain in memory only and are limited to the recent
30-second analysis window.

Advertisement inspection and environment summaries use only the current in-memory scan session.
BLE Activity is an app-defined device-count summary, not an RF interference or spectrum
measurement.

CSV files are generated locally from the current in-memory session and shared only when the user
chooses a destination through Android's share sheet. The app does not upload exports and does not
maintain an export history or database. Some Android-provided BLE addresses included in an
environment export may be randomized by the device or operating system.

## BLE scanning and permissions

The app never starts scanning automatically. On first launch it shows an explanation before
requesting system permissions.

- Android 12 and later: `BLUETOOTH_SCAN` with `neverForLocation`, plus `BLUETOOTH_CONNECT` for
  adapter state and Android-provided device name/address access.
- Android 11 and earlier: legacy Bluetooth permissions and `ACCESS_FINE_LOCATION`, all capped at
  API 30. Android requires location permission for BLE scanning on those platform versions; the
  app does not derive or store the user's location.
- `VIBRATE`: used only for the optional, foreground-only proximity alert.

Normal scanner sessions are bounded to 15, 30, or 60 seconds. Tracking uses BLE advertisement
results only and stops when the tracker is left or the app leaves the foreground. No GATT
connection, foreground service, notification, or background scan is used.

## Privacy design

Scan results, addresses, manufacturer data, service UUIDs, selected devices, and RSSI histories are
not uploaded or automatically stored as app history. A user can explicitly create a temporary CSV
file for the current session and choose its destination through Android's share sheet; the app does
not upload the file or retain an export archive. User settings are stored locally with Preferences DataStore.
The language choice uses Android/AndroidX per-app locale storage. A local SharedPreferences flag
records whether Bluetooth permission was previously requested so
the app does not repeatedly prompt. App backup is disabled and backup/transfer rules exclude app
data.

The in-app Privacy Policy is available from Settings. Public website source is stored in the
repository's `docs/` directory and is served through GitHub Pages after Pages is enabled.

## Requirements

- Android Studio compatible with Android Gradle Plugin 9.3.1
- JDK supported by the included Gradle 9.5.0 wrapper
- Android SDK 37 installed
- Android device or emulator running API 26 or later
- A physical BLE-capable device for meaningful scan and vibration testing

## Build and test

From the project root:

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Release Android App Bundle
./gradlew bundleRelease

# Local JVM tests
./gradlew testDebugUnitTest

# Release lint
./gradlew lintRelease
```

The generated files are normally:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

Install the debug APK through Android Studio or with `adb install` after building it. Grant
Bluetooth permissions on the device and enable Bluetooth through the Android system flow.

## Release readiness

See:

- [`docs/RELEASE_AUDIT.md`](docs/RELEASE_AUDIT.md)
- [`docs/RELEASE_TEST_CHECKLIST.md`](docs/RELEASE_TEST_CHECKLIST.md)
- [`docs/PLAY_STORE_CHECKLIST.md`](docs/PLAY_STORE_CHECKLIST.md)
- [`docs/PLAY_DATA_SAFETY_NOTES.md`](docs/PLAY_DATA_SAFETY_NOTES.md)
