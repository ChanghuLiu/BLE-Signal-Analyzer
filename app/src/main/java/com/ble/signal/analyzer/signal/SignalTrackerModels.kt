package com.ble.signal.analyzer.signal

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

enum class ProximityLabel(val displayName: String) {
    VERY_CLOSE("Very Close"),
    CLOSE("Close"),
    NEARBY("Nearby"),
    WEAK("Weak"),
    VERY_WEAK("Very Weak"),
}

data class SignalTrackerState(
    val deviceId: String? = null,
    val deviceName: String = "Unknown Device",
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
    val unavailableReason: String? = null,
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
