# BLE Signal Analyzer V1.0 Play Store Checklist

## Product identity

- App name: **BLE Signal Analyzer**
- Package/application ID: `com.ble.signal.analyzer`
- Version code: `1`
- Version name: `1.0`
- Suggested category: **Tools**
- Monetization: **Paid app / one-time purchase**
- Ads: **No**
- Account/login: **No**
- Backend/cloud: **No**
- Analytics/telemetry: **No**

## SDK and release configuration

- compileSdk: `37`
- targetSdk: `37`
- minSdk: `26`
- Android Gradle Plugin: `9.3.1`
- Kotlin Compose plugin: `2.2.10`
- Release minification: disabled
- Resource shrinking: disabled

The target API is above Google Play's current API 36 minimum for new phone/tablet submissions.
Android 17/API 37 behavior must still be covered by device/emulator testing.

## Final merged release permissions

- `android.permission.BLUETOOTH` (`maxSdkVersion=30`)
- `android.permission.BLUETOOTH_ADMIN` (`maxSdkVersion=30`)
- `android.permission.ACCESS_FINE_LOCATION` (`maxSdkVersion=30`)
- `android.permission.BLUETOOTH_SCAN` (`neverForLocation`)
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.VIBRATE`
- `com.ble.signal.analyzer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — signature-level internal
  permission added by AndroidX Core for safe non-exported dynamic receiver handling

`android.permission.DUMP` guards the AndroidX Profile Installer receiver but is not requested by
the app. No INTERNET, modern location, background location, notification, camera, microphone,
media/storage, contacts, phone, or broad package-query permission is present.

## Privacy and Data Safety

- In-app Privacy Policy: present under Settings
- External Privacy Policy URL target:
  `https://changhuliu.github.io/BLE-Signal-Analyzer/privacy.html`
- GitHub Pages status: **NOT YET ENABLED — URL MUST BE VERIFIED BEFORE PLAY SUBMISSION**
- Support email: `artbyte@126.com`
- BLE scan processing: local and memory-only
- BLE history: not persisted
- Preferences: app settings use DataStore; language uses Android/AndroidX per-app locale storage;
  one permission-prompt flag uses SharedPreferences
- App backup: disabled with explicit backup/transfer exclusions
- Network upload: none
- Data collection: review the verified technical facts in
  [`PLAY_DATA_SAFETY_NOTES.md`](PLAY_DATA_SAFETY_NOTES.md) against current Play definitions

## Signing and artifacts

- Release signing configuration: **missing (`Config: none`)**
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Both current release artifacts are unsigned and must not be uploaded as production artifacts.

Before upload, create/select a protected upload key, configure release signing or use Android
Studio's signed bundle flow, and verify the resulting AAB signature. Do not commit signing secrets.

## Store listing and Play Console

- [x] Replace `support_email` with a real monitored address
- [x] Add public Privacy Policy source matching the in-app policy
- [ ] Enable GitHub Pages from `main` and `/docs`, then verify every public URL
- [ ] Configure the external Privacy Policy URL in Play Console
- [ ] Complete Data Safety using the final signed AAB and technical notes
- [ ] Create final store description using neutral BLE/RSSI wording
- [ ] Prepare real screenshots and required store graphics
- [ ] Set the app as paid with the intended one-time price in Play Console
- [ ] Complete content rating and target-audience declarations
- [ ] Complete developer identity/account requirements
- [ ] Configure Play App Signing and protect the upload key
- [ ] Upload only a verified signed AAB
- [ ] Complete [`RELEASE_TEST_CHECKLIST.md`](RELEASE_TEST_CHECKLIST.md)
