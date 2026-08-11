package com.ble.signal.analyzer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.ble.signal.analyzer.AppDestination
import com.ble.signal.analyzer.AppUiState
import com.ble.signal.analyzer.BuildConfig
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.scanner.DeviceFilterMode
import com.ble.signal.analyzer.scanner.DeviceSortMode
import com.ble.signal.analyzer.ui.detail.DeviceDetailScreen
import com.ble.signal.analyzer.ui.info.AboutScreen
import com.ble.signal.analyzer.ui.info.HowBleSignalsWorkScreen
import com.ble.signal.analyzer.ui.info.PrivacyPolicyScreen
import com.ble.signal.analyzer.ui.scanner.ScannerScreen
import com.ble.signal.analyzer.ui.settings.SettingsScreen
import com.ble.signal.analyzer.ui.theme.ThemeMode
import com.ble.signal.analyzer.ui.tracker.SignalTrackerScreen

@Composable
fun BleSignalAnalyzerApp(
    uiState: AppUiState,
    onToggleScanning: () -> Unit,
    onFilterApplied: (DeviceFilterMode, Int) -> Unit,
    onSortChanged: (DeviceSortMode) -> Unit,
    onFreezeChanged: (Boolean) -> Unit,
    onDeviceSelected: (BleDeviceInfo) -> Unit,
    onOpenTracker: () -> Unit,
    onResumeTracking: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenHowBleSignalsWork: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    onBackToScanner: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onPermissionNotNow: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onScanDurationChanged: (Int) -> Unit,
    onShowUnnamedChanged: (Boolean) -> Unit,
    onMinimumRssiChanged: (Int) -> Unit,
    onKeepScreenAwakeChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onSignalDescriptionsChanged: (Boolean) -> Unit,
    onProximityAlertThresholdChanged: (Int) -> Unit,
    onProximityAlertEnabledChanged: (Boolean) -> Unit,
    onVibrationConsumed: (Long) -> Unit,
) {
    BackHandler(enabled = uiState.destination != AppDestination.Scanner, onBack = onBack)

    when (uiState.destination) {
        AppDestination.Scanner -> ScannerScreen(
            devices = uiState.visibleDevices,
            totalDeviceCount = uiState.devices.size,
            isScanStarting = uiState.isScanStarting,
            isScanning = uiState.isScanning,
            hasCompletedScan = uiState.hasCompletedScan,
            scanError = uiState.scanError,
            bleSupported = uiState.bleSupported,
            bluetoothEnabled = uiState.bluetoothEnabled,
            permissionState = uiState.permissionState,
            permissionPromptDismissed = uiState.permissionPromptDismissed,
            filterMode = uiState.filterMode,
            sortMode = uiState.sortMode,
            minimumRssi = uiState.minimumRssi,
            activeFilterCount = uiState.activeFilterCount,
            freezeEnabled = uiState.freezeEnabled,
            signalDescriptions = uiState.signalDescriptions,
            onToggleScanning = onToggleScanning,
            onFilterApplied = onFilterApplied,
            onSortChanged = onSortChanged,
            onFreezeChanged = onFreezeChanged,
            onDeviceSelected = onDeviceSelected,
            onOpenSettings = onOpenSettings,
            onRequestBluetoothPermission = onRequestBluetoothPermission,
            onPermissionNotNow = onPermissionNotNow,
            onOpenAppSettings = onOpenAppSettings,
            onEnableBluetooth = onEnableBluetooth,
        )

        AppDestination.DeviceDetail -> uiState.selectedDevice?.let { device ->
            DeviceDetailScreen(
                device = device,
                showSignalDescription = uiState.signalDescriptions,
                onBack = onBack,
                onTrackSignal = onOpenTracker,
            )
        }

        AppDestination.SignalTracker -> uiState.selectedDevice?.let { device ->
            SignalTrackerScreen(
                device = device,
                trackerState = uiState.signalTrackerState,
                showSignalDescription = uiState.signalDescriptions,
                keepScreenAwake = uiState.keepScreenAwake,
                proximityAlertThreshold = uiState.proximityAlertThreshold,
                onProximityAlertThresholdChanged = onProximityAlertThresholdChanged,
                bleSupported = uiState.bleSupported,
                bluetoothEnabled = uiState.bluetoothEnabled,
                permissionState = uiState.permissionState,
                onProximityAlertEnabledChanged = onProximityAlertEnabledChanged,
                onVibrationConsumed = onVibrationConsumed,
                onResumeTracking = onResumeTracking,
                onRequestBluetoothPermission = onRequestBluetoothPermission,
                onOpenAppSettings = onOpenAppSettings,
                onEnableBluetooth = onEnableBluetooth,
                onBack = onBack,
                onBackToScanner = onBackToScanner,
            )
        }

        AppDestination.Settings -> SettingsScreen(
            appVersion = BuildConfig.VERSION_NAME,
            scanDurationSeconds = uiState.scanDurationSeconds,
            showUnnamedDevices = uiState.showUnnamedDevices,
            minimumRssi = uiState.minimumRssi,
            keepScreenAwake = uiState.keepScreenAwake,
            themeMode = uiState.themeMode,
            signalDescriptions = uiState.signalDescriptions,
            proximityAlertThreshold = uiState.proximityAlertThreshold,
            onBack = onBack,
            onScanDurationChanged = onScanDurationChanged,
            onShowUnnamedChanged = onShowUnnamedChanged,
            onMinimumRssiChanged = onMinimumRssiChanged,
            onKeepScreenAwakeChanged = onKeepScreenAwakeChanged,
            onThemeChanged = onThemeChanged,
            onSignalDescriptionsChanged = onSignalDescriptionsChanged,
            onProximityAlertThresholdChanged = onProximityAlertThresholdChanged,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenHowBleSignalsWork = onOpenHowBleSignalsWork,
            onOpenAbout = onOpenAbout,
        )

        AppDestination.PrivacyPolicy -> PrivacyPolicyScreen(onBack = onBack)

        AppDestination.HowBleSignalsWork -> HowBleSignalsWorkScreen(onBack = onBack)

        AppDestination.About -> AboutScreen(
            appVersion = BuildConfig.VERSION_NAME,
            onBack = onBack,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenHowBleSignalsWork = onOpenHowBleSignalsWork,
        )
    }
}
