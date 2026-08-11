# BLE Signal Analyzer V1.0 Release Audit

Audit date: 2026-08-11

Final recommendation: **READY FOR DEVICE TESTING**

The source, release build, unit tests, and release lint are healthy. Play Console submission is not
yet ready because GitHub Pages must be enabled and verified, the external Privacy Policy URL is not
configured in Play Console, and the physical-device checklist remains open.

## 1. App identity

- App name: BLE Signal Analyzer
- Namespace/application ID: `com.ble.signal.analyzer`
- Version code: `1`
- Version name: `1.0`

No old package name, alternate product name, mock product name, or executable Phase 1 sample data
was found. Test-only BLE values remain confined to unit tests. The inactive generic R8 placeholder
comment was removed. The support contact is configured as `artbyte@126.com`.

## 2. SDK configuration

- compileSdk: `37`
- targetSdk: `37`
- minSdk: `26`
- Android Gradle Plugin: `9.3.1`
- Kotlin Compose plugin: `2.2.10`
- Gradle wrapper: `9.5.0`

API 37 is supported by the installed Android 17 SDK and exceeds the current Google Play API 36
target requirement. Android 17 behavior still requires device/emulator validation.

## 3. Final permissions

The merged release manifest requests:

1. `android.permission.BLUETOOTH` — capped at API 30
2. `android.permission.BLUETOOTH_ADMIN` — capped at API 30
3. `android.permission.ACCESS_FINE_LOCATION` — capped at API 30
4. `android.permission.BLUETOOTH_SCAN` — `neverForLocation`
5. `android.permission.BLUETOOTH_CONNECT`
6. `android.permission.VIBRATE`
7. `com.ble.signal.analyzer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — app-specific signature
   permission injected by AndroidX Core for safe non-exported dynamic receiver registration

The app also declares the matching signature permission. The AndroidX Profile Installer receiver
is guarded by `android.permission.DUMP`; that permission is not requested by the app.

No INTERNET, modern location, background location, camera, audio, storage/media, notifications,
contacts, phone, or broad package-query permission is present.

## 4. Network and privacy audit

No application network code or network permission was found. Direct and transitive dependency
review found no Retrofit, OkHttp, Firebase, Crashlytics, analytics, advertising, WebSocket, remote
API, or cloud SDK. DataStore transitively uses Okio for local file I/O; Okio is not an HTTP client.
No production logging calls were found, so BLE addresses, manufacturer bytes, and scan records are
not written to Logcat by app code.

The final direct runtime declarations are the Compose BOM, Activity Compose, AppCompat, Compose
Foundation, Material Icons Core, Material 3, Compose UI, Compose UI Graphics, AndroidX Core KTX,
Preferences DataStore, Lifecycle Runtime KTX, and Lifecycle Runtime Compose. AppCompat provides
standard per-app locale selection and compatibility storage. Unused direct declarations for
Compose tooling previews and Lifecycle ViewModel Compose were removed; debug/test tooling remains
variant-scoped and is not packaged as release functionality. No remaining direct dependency is
clearly unnecessary for the V1 implementation.

The in-app Privacy Policy matches the implementation: BLE scan results are processed locally and
temporarily, no scan result is uploaded or permanently stored, and local settings are disclosed.
The policy now also discloses the permission-requested flag. Application backup is disabled and
the backup/transfer resources exclude all app data domains.

## 5. Local storage audit

BLE devices, selected device state, advertisement fields, and RSSI history exist only in
ViewModel/StateFlow memory. No Room or application database exists.

Preferences DataStore `user_settings` keys:

- `scan_duration_seconds`
- `show_unnamed_devices`
- `minimum_rssi`
- `theme_mode`
- `signal_descriptions`
- `keep_screen_awake_while_tracking`
- `proximity_alert_threshold`

The app language selection is stored by the Android/AndroidX per-app locale mechanism. On older
Android versions, AppCompat auto-storage supplies compatibility persistence; on newer versions,
the platform manages the app locale. This storage contains no BLE scan data.

SharedPreferences `bluetooth_permission_state` contains only `permission_requested`.

## 6. Release build result

- `./gradlew clean`: passed
- `./gradlew assembleRelease`: passed
- `./gradlew bundleRelease`: passed
- minifyEnabled: false (`optimization.enable=false`)
- shrinkResources: false

Lifecycle/resource review confirmed that Stop, timeout, scan failure, Bluetooth OFF, permission
loss, `onPause`, navigation away from Signal Tracker, and ViewModel clearing all reach scanner/job
cleanup. Scanner sessions are limited to 15/30/60 seconds. Tracker scanning is intentionally active
only while its screen is visible in the foreground; Keep Screen Awake is likewise scoped to the
active tracker. No service, foreground service, WorkManager job, notification, or background scan
exists. Physical-device lifecycle stress testing remains required.

Android build tools reported that two AndroidX native libraries could not be stripped and packaged
them unchanged: `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so`. This did not
fail the build.

## 7. Unit test result

`./gradlew testDebugUnitTest`: **54 tests, 0 failures, 0 errors**.

## 8. Lint result

`./gradlew lintRelease`: passed with three dependency/tooling update recommendations:

- Gradle 9.5.0 → 9.7.0
- Compose BOM 2026.02.01 → 2026.06.01
- Kotlin Compose plugin 2.2.10 → 2.4.10

No lint error is suppressed for this audit.

## 9. AAB path

- `app/build/outputs/bundle/release/app-release.aab`
- Size from the audited build: 8,647,608 bytes
- Status: generated successfully

## 10. APK paths

- Debug device-test path after `assembleDebug`: `app/build/outputs/apk/debug/app-debug.apk`

## 11. Remaining blockers

- Enable GitHub Pages from `main` and `/docs`, then verify the public pages.
- Configure `https://changhuliu.github.io/BLE-Signal-Analyzer/privacy.html` in Play Console.
- Complete Play Console Data Safety, content rating, pricing, listing, and developer declarations.

## 12. Manual tests still required

All items in [`RELEASE_TEST_CHECKLIST.md`](RELEASE_TEST_CHECKLIST.md) remain device/manual checks,
especially Bluetooth radio behavior, Android 11/12+ permissions, lifecycle cancellation,
vibration hysteresis, TalkBack, large fonts, light/dark themes, and Android 17 compatibility.

## 13. Final recommendation

**READY FOR DEVICE TESTING**

Automated release verification passes and no code-level release blocker was found. The app is not
yet ready for Play Console upload until GitHub Pages activation, Play Console privacy URL
configuration, and device testing are complete.
