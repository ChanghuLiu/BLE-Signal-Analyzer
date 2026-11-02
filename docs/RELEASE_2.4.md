# BLE Signal Analyzer 2.4 release record

- Package: `com.ble.signal.analyzer`
- Version: `2.4 (6)`; previous Play Production: `2.3 (5)`.
- Change: Settings now shows the count of app-cached temporary CSV exports and lets the user clear those files; exports older than 24 hours are cleaned on app resume and before a new export.
- CSV files remain local and are shared only when the user selects a destination in Android's share sheet. Previously shared copies can remain in destination apps.
- Verification: release signing gate, `testDebugUnitTest` (116/116), `lintRelease`, `bundleRelease`, and `assembleRelease` passed. Release lint initially found plural-resource errors in Arabic, French, Hindi and Brazilian Portuguese; the resources were fixed and release lint then passed.
- The actual Play 2.3 export was previously parsed and checked: 86 rows and 6 columns. That validates the existing export format; it does not substitute for installing this 2.4 release on Play-signed devices.
- AAB SHA-256: `28484ba8ff6089f680bf88eac4cfd4c2737178a72824d74076e7b5862aaa073c`.
- APK SHA-256: `fc9df4893b7ca81b7ad95816bd70c6680b98eede34e217352220bd80e068e1e5`.
- Release certificate SHA-256: `f8ff8b6922212833a69b3c59e94c10c4806968c365f70ad9aba9a3912cd98308`.
- No permission, data-collection or network behavior was added. Cached CSVs may contain user-selected session export data in the app's private cache until cleared or expired.
