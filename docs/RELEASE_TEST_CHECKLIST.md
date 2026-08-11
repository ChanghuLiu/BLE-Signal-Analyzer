# BLE Signal Analyzer V1.0 Release Device Test Checklist

Complete this checklist on physical BLE-capable devices before Play Console submission. Test at
least one Android 12+ device and, where available, one Android 11-or-earlier device.

## Install / Startup

- [ ] Fresh install
- [ ] App launches
- [ ] Correct app name: BLE Signal Analyzer
- [ ] No crash
- [ ] No mock data

## Permissions

- [ ] First-run explanation appears before permission dialog
- [ ] Grant Nearby Devices works
- [ ] Deny works
- [ ] Permanent deny leads to Open Settings
- [ ] Return from Settings updates permission state
- [ ] Android 12+ does not request Location
- [ ] Android 11-or-earlier compatibility path is correct where testable

## Bluetooth

- [ ] Bluetooth OFF state is shown
- [ ] Enable Bluetooth system flow works
- [ ] Cancelling the enable flow leaves the OFF state visible
- [ ] BLE-unsupported state does not crash

## Scanner

- [ ] Scan starts
- [ ] Stop works
- [ ] Default 30-second timeout works
- [ ] 15- and 60-second settings work
- [ ] Repeat scan works
- [ ] Unique device count is correct
- [ ] RSSI updates
- [ ] Unknown devices are handled safely
- [ ] No duplicate cards
- [ ] Empty scan shows No BLE devices found
- [ ] Scan failure permits retry

## Filter / Sort / Freeze

- [ ] Filter works
- [ ] Minimum RSSI works
- [ ] Sort by signal
- [ ] Sort by name
- [ ] Sort by last seen
- [ ] Freeze keeps ordering stable
- [ ] RSSI still updates while frozen

## Device Detail

- [ ] Device name
- [ ] Manufacturer
- [ ] Manufacturer ID
- [ ] Service UUID
- [ ] Tx Power
- [ ] Connectable status
- [ ] Missing fields show Not available
- [ ] Randomized device addresses do not cause issues

## Signal Tracker

- [ ] Real RSSI updates
- [ ] Real graph
- [ ] Stronger trend
- [ ] Weaker trend
- [ ] Stable trend
- [ ] Min / Average / Max
- [ ] Temporary signal loss state
- [ ] Device signal not detected state
- [ ] No exact distance is shown

## Proximity Alert

- [ ] Alert OFF causes no vibration
- [ ] Alert ON works
- [ ] Threshold crossing vibrates once
- [ ] Staying above threshold does not spam
- [ ] Moving below hysteresis re-arms
- [ ] Crossing again vibrates again

## Settings

- [ ] Scan duration persists
- [ ] Minimum RSSI persists
- [ ] Theme persists
- [ ] Show unnamed devices persists
- [ ] Signal descriptions persists
- [ ] Keep screen awake preference persists
- [ ] Alert threshold persists

## UI

- [ ] Light theme
- [ ] Dark theme
- [ ] Small phone
- [ ] Large font
- [ ] Long device names
- [ ] Privacy Policy scrolls
- [ ] About scrolls
- [ ] How BLE Signals Work scrolls
- [ ] TalkBack order and control labels are understandable

## Lifecycle

- [ ] Background while scanning stops scan
- [ ] Return does not auto-scan
- [ ] Background while tracking stops tracking
- [ ] Leaving Signal Tracker stops tracking
- [ ] Bluetooth OFF during scan is safe
- [ ] Bluetooth OFF during tracking is safe
- [ ] Revoking permission while open stops scanner/tracker work safely
- [ ] Repeated scan/stop/tracker/back operations do not leave duplicate callbacks

## Privacy

- [ ] No Internet permission
- [ ] No ads
- [ ] No analytics
- [ ] No account
- [ ] No backend
- [ ] No BLE scan history persists after process termination
- [ ] Settings remain local and are not included in app backup
