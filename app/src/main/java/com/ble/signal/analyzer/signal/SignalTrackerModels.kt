package com.ble.signal.analyzer.signal

import com.ble.signal.analyzer.data.ble.BleScanErrorKind

data class RssiSample(
    val timestamp: Long,
    val rssi: Int,
)

data class SmoothedRssiSample(
    val timestamp: Long,
    val rssi: Double,
)

enum class SignalTrend {
    STRONGER,
    WEAKER,
    STABLE,
    COLLECTING,
}

enum class ProximityLabel {
    VERY_CLOSE,
    CLOSE,
    NEARBY,
    WEAK,
    VERY_WEAK,
}

enum class TrackingUnavailableReason {
    BLE_UNSUPPORTED,
    PERMISSION_REQUIRED,
    PERMISSION_LOST,
    BLUETOOTH_DISABLED,
    APP_BACKGROUNDED,
    SCAN_ALREADY_RUNNING,
    SCAN_REGISTRATION_FAILED,
    SCAN_INTERNAL_ERROR,
    SCAN_FEATURE_UNSUPPORTED,
    SCAN_RESOURCES_UNAVAILABLE,
    SCAN_TOO_FREQUENT,
    SCANNER_UNAVAILABLE,
    SCAN_UNKNOWN,
}

fun BleScanErrorKind.toTrackingUnavailableReason(): TrackingUnavailableReason = when (this) {
    BleScanErrorKind.AlreadyRunning -> TrackingUnavailableReason.SCAN_ALREADY_RUNNING
    BleScanErrorKind.ApplicationRegistrationFailed ->
        TrackingUnavailableReason.SCAN_REGISTRATION_FAILED
    BleScanErrorKind.InternalError -> TrackingUnavailableReason.SCAN_INTERNAL_ERROR
    BleScanErrorKind.FeatureUnsupported ->
        TrackingUnavailableReason.SCAN_FEATURE_UNSUPPORTED
    BleScanErrorKind.HardwareResourcesUnavailable ->
        TrackingUnavailableReason.SCAN_RESOURCES_UNAVAILABLE
    BleScanErrorKind.ScanningTooFrequently -> TrackingUnavailableReason.SCAN_TOO_FREQUENT
    BleScanErrorKind.PermissionRequired -> TrackingUnavailableReason.PERMISSION_REQUIRED
    BleScanErrorKind.BluetoothDisabled -> TrackingUnavailableReason.BLUETOOTH_DISABLED
    BleScanErrorKind.ScannerUnavailable -> TrackingUnavailableReason.SCANNER_UNAVAILABLE
    BleScanErrorKind.Unknown -> TrackingUnavailableReason.SCAN_UNKNOWN
}

data class SignalTrackerState(
    val deviceId: String? = null,
    val deviceAddress: String? = null,
    val deviceName: String = "",
    val currentRssi: Int? = null,
    val smoothedRssi: Double? = null,
    val samples: List<RssiSample> = emptyList(),
    val smoothedSamples: List<SmoothedRssiSample> = emptyList(),
    val minRssi: Int? = null,
    val maxRssi: Int? = null,
    val averageRssi: Int? = null,
    val trend: SignalTrend = SignalTrend.COLLECTING,
    val proximityLabel: ProximityLabel? = null,
    val lastSeen: Long? = null,
    val isSignalStale: Boolean = false,
    val isSignalLost: Boolean = false,
    val isTracking: Boolean = false,
    val trackingStartedAt: Long? = null,
    val graphTimeMillis: Long = 0L,
    val unavailableReason: TrackingUnavailableReason? = null,
    val proximityAlertEnabled: Boolean = false,
    val proximityAlertStatus: ProximityAlertStatus = ProximityAlertStatus.DISABLED,
    val pendingVibrationEventId: Long? = null,
)

object SignalTrackerConfig {
    const val GRAPH_WINDOW_MILLIS = 30_000L
    const val STALE_AFTER_MILLIS = 5_000L
    const val LOST_AFTER_MILLIS = 15_000L
    const val TREND_WINDOW_MILLIS = 2_500L
    const val TREND_THRESHOLD_DBM = 3.0
    const val MAX_GRAPH_SAMPLES = 600
    const val STATUS_TICK_MILLIS = 1_000L
    const val ALERT_HYSTERESIS_DBM = 5.0
    const val ALERT_COOLDOWN_MILLIS = 4_000L
    const val ALERT_VIBRATION_DURATION_MILLIS = 150L

    // A fixed 0.30 EMA weight smooths BLE noise while remaining responsive to movement.
    const val EMA_ALPHA = 0.30
}
