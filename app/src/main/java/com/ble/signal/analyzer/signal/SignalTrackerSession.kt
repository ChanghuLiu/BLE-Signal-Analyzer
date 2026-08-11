package com.ble.signal.analyzer.signal

import com.ble.signal.analyzer.model.BleDeviceInfo

data class SignalAvailability(
    val isWaiting: Boolean,
    val isLost: Boolean,
)

object SignalAvailabilityCalculator {
    fun calculate(
        lastSeenMillis: Long?,
        trackingStartedAtMillis: Long?,
        nowMillis: Long,
    ): SignalAvailability {
        val referenceTime = when {
            lastSeenMillis != null && trackingStartedAtMillis != null ->
                maxOf(lastSeenMillis, trackingStartedAtMillis)
            lastSeenMillis != null -> lastSeenMillis
            trackingStartedAtMillis != null -> trackingStartedAtMillis
            else -> nowMillis
        }
        val elapsedMillis = (nowMillis - referenceTime).coerceAtLeast(0L)
        return SignalAvailability(
            isWaiting = elapsedMillis >= SignalTrackerConfig.STALE_AFTER_MILLIS,
            isLost = elapsedMillis >= SignalTrackerConfig.LOST_AFTER_MILLIS,
        )
    }
}

/** Pure selected-device session logic shared by the ViewModel and unit tests. */
object SignalTrackerSession {
    fun start(
        device: BleDeviceInfo,
        nowMillis: Long,
        isTracking: Boolean,
        unavailableReason: TrackingUnavailableReason?,
    ): SignalTrackerState = SignalTrackerState(
        deviceId = device.id,
        deviceAddress = device.address,
        deviceName = device.name?.trim().orEmpty(),
        currentRssi = device.rssi,
        lastSeen = device.lastSeen.takeIf { it > 0L },
        isTracking = isTracking,
        trackingStartedAt = nowMillis,
        graphTimeMillis = nowMillis,
        unavailableReason = unavailableReason,
    )

    fun availability(
        state: SignalTrackerState,
        nowMillis: Long,
    ): SignalAvailability {
        return SignalAvailabilityCalculator.calculate(
            lastSeenMillis = state.lastSeen,
            trackingStartedAtMillis = state.trackingStartedAt,
            nowMillis = nowMillis,
        )
    }

    fun matchesSelectedDevice(
        state: SignalTrackerState,
        incomingDeviceId: String,
        incomingAddress: String?,
    ): Boolean {
        if (state.deviceId != null && incomingDeviceId == state.deviceId) return true
        val selectedAddress = state.deviceAddress?.takeIf { it.isNotBlank() } ?: return false
        return incomingAddress?.equals(selectedAddress, ignoreCase = true) == true
    }

    fun onSelectedAdvertisement(
        state: SignalTrackerState,
        incomingDeviceId: String,
        incomingAddress: String?,
        timestampMillis: Long,
    ): SignalTrackerState {
        if (!matchesSelectedDevice(state, incomingDeviceId, incomingAddress)) return state
        return state.copy(
            lastSeen = timestampMillis,
            isSignalStale = false,
            isSignalLost = false,
        )
    }
}
