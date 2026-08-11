package com.ble.signal.analyzer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ble.signal.analyzer.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings",
)

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::preferencesToSettings)

    suspend fun setScanDuration(seconds: Int) = update(Keys.ScanDuration) {
        SettingsValueNormalizer.scanDuration(seconds)
    }

    suspend fun setShowUnnamedDevices(show: Boolean) = update(Keys.ShowUnnamedDevices) { show }

    suspend fun setMinimumRssi(rssi: Int) = update(Keys.MinimumRssi) {
        SettingsValueNormalizer.minimumRssi(rssi)
    }

    suspend fun setThemeMode(themeMode: ThemeMode) = update(Keys.ThemeMode) { themeMode.name }

    suspend fun setSignalDescriptions(show: Boolean) = update(Keys.SignalDescriptions) { show }

    suspend fun setKeepScreenAwake(keepAwake: Boolean) = update(Keys.KeepScreenAwake) {
        keepAwake
    }

    suspend fun setProximityAlertThreshold(rssi: Int) = update(Keys.ProximityAlertThreshold) {
        SettingsValueNormalizer.proximityAlertThreshold(rssi)
    }

    private suspend fun <T> update(key: Preferences.Key<T>, value: () -> T) {
        dataStore.edit { preferences -> preferences[key] = value() }
    }

    private object Keys {
        val ScanDuration = intPreferencesKey("scan_duration_seconds")
        val ShowUnnamedDevices = booleanPreferencesKey("show_unnamed_devices")
        val MinimumRssi = intPreferencesKey("minimum_rssi")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val SignalDescriptions = booleanPreferencesKey("signal_descriptions")
        val KeepScreenAwake = booleanPreferencesKey("keep_screen_awake_while_tracking")
        val ProximityAlertThreshold = intPreferencesKey("proximity_alert_threshold")
    }

    private fun preferencesToSettings(preferences: Preferences): AppSettings = AppSettings(
        scanDurationSeconds = SettingsValueNormalizer.scanDuration(
            preferences[Keys.ScanDuration] ?: AppSettings.DEFAULT_SCAN_DURATION_SECONDS,
        ),
        showUnnamedDevices = preferences[Keys.ShowUnnamedDevices] ?: true,
        minimumRssi = SettingsValueNormalizer.minimumRssi(
            preferences[Keys.MinimumRssi] ?: AppSettings.DEFAULT_MINIMUM_RSSI,
        ),
        themeMode = SettingsValueNormalizer.themeMode(preferences[Keys.ThemeMode]),
        signalDescriptions = preferences[Keys.SignalDescriptions] ?: true,
        keepScreenAwake = preferences[Keys.KeepScreenAwake] ?: true,
        proximityAlertThreshold = SettingsValueNormalizer.proximityAlertThreshold(
            preferences[Keys.ProximityAlertThreshold]
                ?: AppSettings.DEFAULT_PROXIMITY_ALERT_THRESHOLD,
        ),
    )
}
