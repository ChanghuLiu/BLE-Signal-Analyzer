package com.ble.signal.analyzer.data.settings

import com.ble.signal.analyzer.ui.theme.ThemeMode

data class AppSettings(
    val scanDurationSeconds: Int = DEFAULT_SCAN_DURATION_SECONDS,
    val showUnnamedDevices: Boolean = true,
    val minimumRssi: Int = DEFAULT_MINIMUM_RSSI,
    val themeMode: ThemeMode = ThemeMode.System,
    val signalDescriptions: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val proximityAlertThreshold: Int = DEFAULT_PROXIMITY_ALERT_THRESHOLD,
) {
    companion object {
        const val DEFAULT_SCAN_DURATION_SECONDS = 30
        const val DEFAULT_MINIMUM_RSSI = -100
        const val DEFAULT_PROXIMITY_ALERT_THRESHOLD = -50
        val VALID_SCAN_DURATIONS = setOf(15, 30, 60)
    }
}

object SettingsValueNormalizer {
    fun scanDuration(value: Int): Int = value.takeIf {
        it in AppSettings.VALID_SCAN_DURATIONS
    } ?: AppSettings.DEFAULT_SCAN_DURATION_SECONDS

    fun minimumRssi(value: Int): Int = value.coerceIn(-100, -30)

    fun proximityAlertThreshold(value: Int): Int = value.coerceIn(-90, -35)

    fun themeMode(value: String?): ThemeMode = ThemeMode.entries.firstOrNull {
        it.name == value
    } ?: ThemeMode.System
}
