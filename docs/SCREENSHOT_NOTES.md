# BLE Signal Analyzer Play Store Screenshot Notes

## Capture provenance

All screenshots were captured from the real BLE Signal Analyzer app running on a physical Samsung
SM-J327W (Android API 27) on 2026-08-11. The app language was set to English. No UI was generated,
reconstructed, or populated with mock data.

The phone's native capture resolution was 720 x 1280. Each final Play asset was uniformly scaled by
1.5x to 1080 x 1920 using deterministic bicubic resampling and saved as PNG. No captions,
marketing overlays, device frames, false claims, or other visual elements were added.

## Source and final files

| # | Real source capture | Screen/content | Final file | Final dimensions |
|---|---|---|---|---|
| 1 | `play-store-screenshots/source/source_01_scanner_720x1280.png` | Active BLE scan with 39 real nearby results | `play-store-screenshots/play_screenshot_01_scanner.png` | 1080 x 1920 |
| 2 | `play-store-screenshots/source/source_02_device_detail_720x1280.png` | Real Apple advertisement detail at -48 dBm | `play-store-screenshots/play_screenshot_02_device_detail.png` | 1080 x 1920 |
| 3 | `play-store-screenshots/source/source_03_signal_tracker_720x1280.png` | Live selected-device RSSI and rolling graph | `play-store-screenshots/play_screenshot_03_signal_tracker.png` | 1080 x 1920 |
| 4 | `play-store-screenshots/source/source_04_settings_720x1280.png` | Settings with theme, language, signal labels, and Privacy Policy access | `play-store-screenshots/play_screenshot_04_settings.png` | 1080 x 1920 |
| 5 | `play-store-screenshots/source/source_05_privacy_720x1280.png` | In-app Privacy Policy | `play-store-screenshots/play_screenshot_05_privacy.png` | 1080 x 1920 |

## Recommended Play Console upload set

Upload these four in this order:

1. `play_screenshot_01_scanner.png`
2. `play_screenshot_02_device_detail.png`
3. `play_screenshot_03_signal_tracker.png`
4. `play_screenshot_04_settings.png`

`play_screenshot_05_privacy.png` is an optional fifth screenshot if the listing should show the
local-processing and privacy disclosure directly.

## Compliance notes

- Format: PNG
- Aspect ratio: 9:16
- Final dimensions: 1080 x 1920
- Both sides meet the 1080-pixel promotion-eligibility target.
- Screens show only actual app UI and real BLE scan/advertisement data.
- No exact-distance, exact-location, hidden-tracker, spy-device, or people-tracking claim appears.
