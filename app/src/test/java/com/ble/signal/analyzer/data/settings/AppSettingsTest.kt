package com.ble.signal.analyzer.data.settings

import com.ble.signal.analyzer.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaults_matchProductRequirements() {
        val settings = AppSettings()

        assertEquals(30, settings.scanDurationSeconds)
        assertTrue(settings.showUnnamedDevices)
        assertEquals(-100, settings.minimumRssi)
        assertEquals(ThemeMode.System, settings.themeMode)
        assertTrue(settings.signalDescriptions)
        assertTrue(settings.keepScreenAwake)
        assertEquals(-50, settings.proximityAlertThreshold)
    }

    @Test
    fun scanDuration_acceptsOnlySupportedValues() {
        assertEquals(15, SettingsValueNormalizer.scanDuration(15))
        assertEquals(30, SettingsValueNormalizer.scanDuration(30))
        assertEquals(60, SettingsValueNormalizer.scanDuration(60))
        assertEquals(30, SettingsValueNormalizer.scanDuration(45))
    }

    @Test
    fun rssiPreferences_areClampedToTheirSupportedRanges() {
        assertEquals(-100, SettingsValueNormalizer.minimumRssi(-110))
        assertEquals(-30, SettingsValueNormalizer.minimumRssi(-20))
        assertEquals(-90, SettingsValueNormalizer.proximityAlertThreshold(-100))
        assertEquals(-35, SettingsValueNormalizer.proximityAlertThreshold(-20))
    }

    @Test
    fun themeMapping_restoresKnownValueAndFallsBackSafely() {
        assertEquals(ThemeMode.Dark, SettingsValueNormalizer.themeMode("Dark"))
        assertEquals(ThemeMode.System, SettingsValueNormalizer.themeMode("unknown"))
        assertEquals(ThemeMode.System, SettingsValueNormalizer.themeMode(null))
    }
}
