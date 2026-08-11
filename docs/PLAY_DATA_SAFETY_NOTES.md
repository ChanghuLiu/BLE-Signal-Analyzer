# Google Play Data Safety Technical Notes

These notes describe the verified V2.0 implementation. They are supporting facts for completing
Google Play's Data Safety form, not automatic answers to every Play Console question. Re-check the
current Play definitions and the final signed artifact before submission.

## Accounts and identity

- The app has no account system and no login.
- It does not create or transmit user identifiers.
- It has no advertising ID integration.

## Network and third-party services

- The merged release manifest has no `INTERNET` permission.
- Source and dependency audits found no backend API, HTTP client, Firebase, Crashlytics,
  analytics, advertising, WebSocket, or cloud SDK.
- The app does not upload BLE scan results, device addresses, settings, or exported files.

## BLE data processed on the device

The app temporarily processes Android BLE advertisement fields, including device name/address,
RSSI, manufacturer data, service UUIDs, Tx Power, connectable state, and last-seen time. This data
is used only to render the scanner, detail, and active tracker screens.

- BLE device results are held in `StateFlow`-backed in-memory UI state.
- RSSI and smoothed RSSI samples are held in a rolling in-memory window.
- No BLE device address, scan history, RSSI history, manufacturer data, service UUID history, or
  selected device is automatically written as application history.
- No Room or application database is used.

## User-initiated CSV export

- CSV export occurs only after an explicit user action in Signal Tracker, Compare Devices, or BLE
  Environment.
- The app generates the file locally in its private cache and exposes only the export cache
  directory through a non-exported AndroidX FileProvider.
- The app does not upload the file. Android's Sharesheet transfers it only to the destination
  explicitly selected by the user.
- Export cache files older than 24 hours are deleted when another export is generated.
- The app stores no export index, archive, database, or history. A destination chosen by the user
  may retain its own copy outside the app's control.

## Local settings

Preferences DataStore file `user_settings` stores only:

- `scan_duration_seconds`
- `show_unnamed_devices`
- `minimum_rssi`
- `theme_mode`
- `signal_descriptions`
- `keep_screen_awake_while_tracking`
- `proximity_alert_threshold`

The Android/AndroidX per-app locale mechanism stores the selected app language locally. This
locale preference contains no BLE device or signal data.

SharedPreferences file `bluetooth_permission_state` stores one local control flag:

- `permission_requested`

This flag prevents repeated automatic permission prompts and contains no BLE device data.

The application sets `allowBackup=false`; both legacy and Android 12+ extraction rules exclude app
files, preferences, databases, and external app data from backup. Some Android device vendors may
control device-to-device migration at the platform level, so final Play Console wording should be
checked against the behavior of supported devices and current policy definitions.

## Permissions and purpose

- `BLUETOOTH_SCAN`: core nearby BLE advertisement scanning; marked `neverForLocation`.
- `BLUETOOTH_CONNECT`: reads Bluetooth adapter state and Android-provided device name/address.
- Legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION`: capped at API 30 for the
  Android 11-and-earlier BLE compatibility path.
- `VIBRATE`: optional foreground proximity alert.
- CSV sharing uses a temporary FileProvider URI and requires no storage or Internet permission.
- The AndroidX Core signature-level dynamic-receiver permission protects an internal non-exported
  receiver registration path and does not expose user data.

## Submission review points

- Verify the final release AAB has the same merged permissions.
- Review whether on-device-only processing is outside "collected" under the current Data Safety
  definitions instead of relying on this document alone.
- Support email is configured as `artbyte@126.com`.
- Enable GitHub Pages and verify the public Privacy Policy URL before submission.
