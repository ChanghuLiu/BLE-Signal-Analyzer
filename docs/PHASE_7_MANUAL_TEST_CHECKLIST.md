# Phase 7 Manual UI Checklist

Run these checks on at least one normal-size Android phone. Repeat the content and navigation
checks with a large system font setting because emulator/JVM tests cannot validate final text
measurement or TalkBack behavior completely.

- [ ] Scanner is readable and balanced in Light theme.
- [ ] Scanner is readable and balanced in Dark theme.
- [ ] Device Detail is readable in both themes.
- [ ] Signal Tracker graph, trend, and statistics are readable in both themes.
- [ ] Settings rows and selection dialogs are readable in both themes.
- [ ] Privacy Policy opens from Settings and scrolls to the contact field.
- [ ] About opens from Settings, scrolls, and links to both information pages.
- [ ] How BLE Signals Work opens and scrolls through all examples and steps.
- [ ] Large system font does not clip critical buttons, settings, or technical values.
- [ ] Settings, Back, and other icon-only buttons have meaningful TalkBack labels.
- [ ] Device cards read name, manufacturer, RSSI, quality, and last-seen in logical order.
- [ ] Signal quality has visible text with the default Signal descriptions setting.
- [ ] Signal quality remains available to accessibility services if descriptions are disabled.
- [ ] Trend always includes stronger, weaker, stable, or collecting text.
- [ ] Empty, permission, Bluetooth-off, scan-error, and device-loss states are readable.
- [ ] Switch rows can be toggled by tapping their labels and announce their state.
- [ ] Filter, Sort, and Freeze controls announce their active state clearly.
- [ ] No device-location, hidden-tracker, people-tracking, or exact-distance capability claim appears.
- [ ] The app name is consistently shown as BLE Signal Analyzer.
- [ ] Settings and About both show the installed `versionName`.
- [x] The support email is configured as `artbyte@126.com`.
- [ ] Manifest permissions are unchanged except for the Phase 6 `VIBRATE` permission.
