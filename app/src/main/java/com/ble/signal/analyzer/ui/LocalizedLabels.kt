package com.ble.signal.analyzer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.data.ble.BleManufacturerLookup
import com.ble.signal.analyzer.data.ble.BleScanErrorKind
import com.ble.signal.analyzer.model.SignalQuality
import com.ble.signal.analyzer.scanner.DeviceFilterMode
import com.ble.signal.analyzer.scanner.DeviceSortMode
import com.ble.signal.analyzer.signal.ProximityLabel
import com.ble.signal.analyzer.signal.SignalStabilityLabel
import com.ble.signal.analyzer.signal.TrackingUnavailableReason

@Composable
fun signalQualityLabel(quality: SignalQuality): String = stringResource(
    when (quality) {
        SignalQuality.Excellent -> R.string.signal_quality_excellent
        SignalQuality.Strong -> R.string.signal_quality_strong
        SignalQuality.Good -> R.string.signal_quality_good
        SignalQuality.Fair -> R.string.signal_quality_fair
        SignalQuality.Weak -> R.string.signal_quality_weak
    },
)

@Composable
fun proximityLabel(label: ProximityLabel): String = stringResource(
    when (label) {
        ProximityLabel.VERY_CLOSE -> R.string.proximity_very_close
        ProximityLabel.CLOSE -> R.string.proximity_close
        ProximityLabel.NEARBY -> R.string.proximity_nearby
        ProximityLabel.WEAK -> R.string.proximity_weak
        ProximityLabel.VERY_WEAK -> R.string.proximity_very_weak
    },
)

@Composable
fun signalStabilityLabel(label: SignalStabilityLabel): String = stringResource(
    when (label) {
        SignalStabilityLabel.COLLECTING -> R.string.stability_collecting
        SignalStabilityLabel.EXCELLENT -> R.string.stability_excellent
        SignalStabilityLabel.STABLE -> R.string.stability_stable
        SignalStabilityLabel.VARIABLE -> R.string.stability_variable
        SignalStabilityLabel.UNSTABLE -> R.string.stability_unstable
    },
)

@Composable
fun filterModeLabel(mode: DeviceFilterMode): String = stringResource(
    when (mode) {
        DeviceFilterMode.All -> R.string.filter_all_devices
        DeviceFilterMode.Named -> R.string.filter_named_devices
        DeviceFilterMode.Strong -> R.string.filter_strong_signals
        DeviceFilterMode.Connectable -> R.string.filter_connectable_devices
    },
)

@Composable
fun sortModeLabel(mode: DeviceSortMode): String = stringResource(
    when (mode) {
        DeviceSortMode.SignalStrength -> R.string.sort_signal_strength
        DeviceSortMode.DeviceName -> R.string.sort_device_name
        DeviceSortMode.LastSeen -> R.string.sort_last_seen
    },
)

@Composable
fun sortModeShortLabel(mode: DeviceSortMode): String = stringResource(
    when (mode) {
        DeviceSortMode.SignalStrength -> R.string.sort_short_signal
        DeviceSortMode.DeviceName -> R.string.sort_short_name
        DeviceSortMode.LastSeen -> R.string.sort_short_recent
    },
)

@Composable
fun manufacturerDisplayName(manufacturerId: Int?): String = when (manufacturerId) {
    null -> stringResource(R.string.not_available)
    else -> BleManufacturerLookup.nameFor(manufacturerId)
        ?: stringResource(R.string.unknown_manufacturer)
}

@Composable
fun scanErrorMessage(kind: BleScanErrorKind): String = stringResource(
    when (kind) {
        BleScanErrorKind.AlreadyRunning -> R.string.scan_error_already_running
        BleScanErrorKind.ApplicationRegistrationFailed ->
            R.string.scan_error_registration_failed
        BleScanErrorKind.InternalError -> R.string.scan_error_internal
        BleScanErrorKind.FeatureUnsupported -> R.string.scan_error_feature_unsupported
        BleScanErrorKind.HardwareResourcesUnavailable ->
            R.string.scan_error_resources_unavailable
        BleScanErrorKind.ScanningTooFrequently -> R.string.scan_error_too_frequent
        BleScanErrorKind.PermissionRequired -> R.string.scan_error_permission_required
        BleScanErrorKind.BluetoothDisabled -> R.string.scan_error_bluetooth_disabled
        BleScanErrorKind.ScannerUnavailable -> R.string.scan_error_scanner_unavailable
        BleScanErrorKind.Unknown -> R.string.scan_error_unknown
    },
)

@Composable
fun trackingUnavailableMessage(reason: TrackingUnavailableReason): String = stringResource(
    when (reason) {
        TrackingUnavailableReason.BLE_UNSUPPORTED -> R.string.ble_not_supported
        TrackingUnavailableReason.PERMISSION_REQUIRED ->
            R.string.tracking_permission_required
        TrackingUnavailableReason.PERMISSION_LOST -> R.string.tracking_permission_lost
        TrackingUnavailableReason.BLUETOOTH_DISABLED -> R.string.bluetooth_turned_off
        TrackingUnavailableReason.APP_BACKGROUNDED -> R.string.tracking_paused_background
        TrackingUnavailableReason.SCAN_ALREADY_RUNNING -> R.string.scan_error_already_running
        TrackingUnavailableReason.SCAN_REGISTRATION_FAILED ->
            R.string.scan_error_registration_failed
        TrackingUnavailableReason.SCAN_INTERNAL_ERROR -> R.string.scan_error_internal
        TrackingUnavailableReason.SCAN_FEATURE_UNSUPPORTED ->
            R.string.scan_error_feature_unsupported
        TrackingUnavailableReason.SCAN_RESOURCES_UNAVAILABLE ->
            R.string.scan_error_resources_unavailable
        TrackingUnavailableReason.SCAN_TOO_FREQUENT -> R.string.scan_error_too_frequent
        TrackingUnavailableReason.SCANNER_UNAVAILABLE ->
            R.string.scan_error_scanner_unavailable
        TrackingUnavailableReason.SCAN_UNKNOWN -> R.string.scan_error_unknown
    },
)
