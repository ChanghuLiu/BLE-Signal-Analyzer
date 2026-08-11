# Phase 6 Manual Test Checklist

Use a physical Android device because BLE radio, permission, Bluetooth-enable, and vibration
behavior cannot be fully validated by local JVM tests.

- [ ] Fresh install shows the permission explanation before any system dialog.
- [ ] Allow Bluetooth Scan opens the system permission request.
- [ ] Granting permission returns to a ready-to-scan state without starting automatically.
- [ ] Denying permission shows Grant Permission and Not Now.
- [ ] Permanent denial shows Open Settings and does not reopen settings automatically.
- [ ] Granting permission in App Settings is detected after returning to the app.
- [ ] Bluetooth OFF before scanning shows Enable Bluetooth.
- [ ] Accepting the system Bluetooth request returns to ready-to-scan.
- [ ] Cancelling the system Bluetooth request leaves the Bluetooth-off state visible.
- [ ] Scan starts once and rapid repeated start input does not create duplicate callbacks.
- [ ] Stop immediately releases the scan and permits a later scan.
- [ ] The configured 15/30/60-second timeout stops the scan once.
- [ ] Turning Bluetooth OFF during a scan stops it and cancels its timeout.
- [ ] Backgrounding during a scan stops it; foreground return does not restart it.
- [ ] Signal Tracker receives only the selected device's RSSI samples.
- [ ] A missing signal shows the waiting state at about 5 seconds.
- [ ] A missing signal shows the not-detected state at about 15 seconds.
- [ ] Bluetooth OFF in Tracker preserves the graph and offers Enable Bluetooth.
- [ ] Bluetooth ON does not restart tracking until Resume Tracking is pressed.
- [ ] Enabling vibration below the threshold arms the alert.
- [ ] Crossing the threshold with smoothed RSSI produces one short vibration.
- [ ] Remaining above the threshold does not repeat vibration.
- [ ] Falling at least 5 dBm below the threshold re-arms the alert.
- [ ] A second valid crossing after cooldown produces one more vibration.
- [ ] Scan failures leave the UI out of Scanning state and allow Try Again.
- [ ] Repeated scan/stop/tracker/back operations do not crash.
