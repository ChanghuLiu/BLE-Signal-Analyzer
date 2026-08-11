# BLE Signal Analyzer

BLE Signal Analyzer is a local-only Android utility for scanning nearby Bluetooth Low Energy
advertisements and observing relative RSSI changes in real time. It displays advertised device,
manufacturer, service UUID, Tx Power, and connectable information when those fields are available.

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
- Pure Kotlin helpers for filtering, sorting, RSSI smoothing, trend calculation, statistics, and
  proximity-alert evaluation

BLE devices and RSSI samples live only in memory. There is no Room database, backend, account,
cloud sync, advertising, analytics, or network communication.

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
not uploaded or permanently stored. User settings are stored locally with Preferences DataStore.
A local SharedPreferences flag records whether Bluetooth permission was previously requested so
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

# Unsigned release APK unless release signing is configured
./gradlew assembleRelease

# Unsigned release Android App Bundle unless release signing is configured
./gradlew bundleRelease

# Local JVM tests
./gradlew testDebugUnitTest

# Release lint
./gradlew lintRelease
```

The generated files are normally:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Unsigned release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

Install the debug APK through Android Studio or with `adb install` after building it. Grant
Bluetooth permissions on the device and enable Bluetooth through the Android system flow.

## Release signing

The project does not contain a production signing configuration or signing secrets. Before a Play
Console upload:

1. Create or select a protected upload keystore using Android Studio's **Generate Signed App
   Bundle or APK** flow.
2. Keep the keystore and passwords outside version control.
3. Configure the release variant or use the Android Studio signing flow to sign the AAB with the
   upload key.
4. Enable or use Google Play App Signing and retain the upload key securely.
5. Verify the signed bundle before uploading it.

Never commit a keystore, private key, password, `local.properties`, or secret-bearing
`gradle.properties` file.

## Release readiness

See:

- [`docs/RELEASE_AUDIT.md`](docs/RELEASE_AUDIT.md)
- [`docs/RELEASE_TEST_CHECKLIST.md`](docs/RELEASE_TEST_CHECKLIST.md)
- [`docs/PLAY_STORE_CHECKLIST.md`](docs/PLAY_STORE_CHECKLIST.md)
- [`docs/PLAY_DATA_SAFETY_NOTES.md`](docs/PLAY_DATA_SAFETY_NOTES.md)
