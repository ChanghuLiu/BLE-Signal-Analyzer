# BLE Signal Analyzer V2 Release Audit

Audit date: 2026-08-11
Recommendation: **BLOCKED**

## Identity

- Application ID/package: `com.ble.signal.analyzer`
- `versionCode`: `2`
- `versionName`: `2.0`
- Support email: `artbyte@126.com`
- Test handset: Samsung SM-J327W, Android API 27, 720x1280, physical BLE advertisements

## Automated verification

- `./gradlew clean assembleDebug testDebugUnitTest lintDebug`: passed.
- Unit tests: 116 tests, 0 failures/errors/skips.
- Debug lint: 0 errors; four existing dependency/qualifier warnings.
- `./gradlew assembleRelease bundleRelease lintRelease`: first lint worker run hit the transient `BuiltinsVirtualFileProvider duplicated` Gradle/lint worker crash. After `./gradlew --stop`, the same release tasks were rerun with `--no-configuration-cache --max-workers=1` and passed.
- Release lint: 0 errors; the same four existing warnings.
- `git diff --check`: passed.

## Physical regression

The V1 scanner flow was exercised on real BLE devices: permission and Bluetooth on/off states, bounded and repeated scans, unique results, filter, sort, freeze, detail, tracker graph/statistics, proximity settings, theme, language, privacy, about, and help.

Compare Devices was exercised with two real devices. Both RSSI streams and graph lines updated, signal difference and stronger-device text updated, and role labels remained distinct for unnamed devices. Stability initially showed `Collecting data…`, then produced observed `Unstable`, `Stable`, `Variable`, and `Excellent Stability` states with standard deviation/range values in range. A real Device A loss displayed independently while Device B was not marked lost; the inverse physical loss was not reproducibly induced with the available devices, but both independent-loss cases are covered by unit tests.

Advertisement Inspector displayed real name/address/RSSI, Apple manufacturer and ID/data, service/Tx/flags unavailable states, raw uppercase hexadecimal bytes, and the copy action. Clipboard paste verification showed uppercase hex. BLE Environment summarized a real scan session (31 unique devices in one run) with named/unknown/connectable counts, five signal buckets, activity text, and session-only wording.

## CSV export and sharing

- Signal Tracker export produced a real UTF-8 CSV with header `timestamp_iso,elapsed_ms,raw_rssi_dbm,smoothed_rssi_dbm` and real RSSI samples.
- Compare export produced normalized rows with header `timestamp_iso,elapsed_ms,device_role,device_name,raw_rssi_dbm,smoothed_rssi_dbm`. A physical export had independent Device A/Device B rows (229 lines total, 14 A and 214 B) in chronological timestamp order; no interpolated rows were present.
- Environment export produced one row per unique device with the required English schema and stable quality identifiers; no raw advertisement bytes were included.
- Android Sharesheet opened for all three exports. Gmail and Bluetooth accepted the share intent; the configured Email target opened its setup screen. A spreadsheet application was installed on the handset but did not register as a CSV open/share target, so spreadsheet import could not be completed on that device.
- Export files remained readable after sharing and were retained in `cache/export/`; cleanup is age-based (24 hours) before a later export, not immediate. FileProvider exposes only `cache/export/`.
- Real advertising names available during testing were English/ASCII; CSV escaping and non-ASCII cases are covered by unit tests, but a non-English physical device name was not available.

## Localization, RTL, and font size

English, French, Simplified Chinese, and Arabic V2 screens were exercised. Arabic Compare, Inspector, and Environment layouts mirror correctly; graph axes remain technically LTR and hexadecimal byte order remains normal. The language selector is a vertically scrollable container with all 13 languages reachable on 720x1280. At system font scale 1.3, the selector still reached Arabic/Hindi, Compare and Inspector scrolled to their export/raw sections, and Environment content remained scrollable. The handset was restored to font scale 1.0 and English after testing.

## Release manifest and privacy

Final merged release permissions are:

- `BLUETOOTH` (maxSdk 30)
- `BLUETOOTH_ADMIN` (maxSdk 30)
- `ACCESS_FINE_LOCATION` (maxSdk 30)
- `BLUETOOTH_SCAN` with `neverForLocation`
- `BLUETOOTH_CONNECT`
- `VIBRATE`
- AndroidX signature-protected dynamic receiver permission

No INTERNET, background-location, notification, camera, microphone, storage/media, or `MANAGE_EXTERNAL_STORAGE` permission is present. Components include the launcher activity, AndroidX initialization/profile components, and FileProvider authority `com.ble.signal.analyzer.export-file-provider` (`exported=false`, temporary grants enabled). `export_file_paths.xml` exposes only `cache/export/`.

Local `docs/privacy.html`, in-app Privacy Policy, and `docs/PLAY_DATA_SAFETY_NOTES.md` contain the user-initiated/local CSV wording, no-upload/no-history/no-backend/no-ads/no-analytics claims. The live GitHub Pages page returned HTTP 200 but does not yet contain the CSV export wording. The local privacy change is uncommitted and therefore unpublished.

## Artifacts and signing

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (15,447,155 bytes; Android debug signer)
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (10,719,619 bytes; unsigned)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (9,936,998 bytes; unsigned)

## Git state and blockers

Compare/Stability is in commit `75de47d` (`Add BLE signal comparison and stability analysis`). Inspector, Environment, CSV export, localization, privacy, release notes, and related tests remain modified/untracked in the working tree, along with pre-existing `.idea/` and `app/release/` entries. Nothing was discarded, committed, or pushed during this audit.

The release is **BLOCKED** until the release code/docs are intentionally committed, a release signing configuration produces verified APK/AAB signatures, and the updated public privacy page is published. Physical inverse-loss and spreadsheet-import checks should also be repeated when a controllable second advertiser and an operational spreadsheet share target are available.
