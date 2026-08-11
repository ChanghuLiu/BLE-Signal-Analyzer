package com.ble.signal.analyzer.signal

import com.ble.signal.analyzer.model.BleDeviceInfo
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ComparedDevice {
    DEVICE_A,
    DEVICE_B,
}

enum class StrongerSignal {
    DEVICE_A,
    DEVICE_B,
    SIMILAR,
    UNAVAILABLE,
}

data class SignalComparisonResult(
    val differenceDb: Int?,
    val strongerSignal: StrongerSignal,
)

object SignalComparisonCalculator {
    const val SIMILAR_THRESHOLD_DB = 3.0

    fun calculate(
        smoothedRssiA: Double?,
        smoothedRssiB: Double?,
    ): SignalComparisonResult {
        if (smoothedRssiA == null || smoothedRssiB == null) {
            return SignalComparisonResult(null, StrongerSignal.UNAVAILABLE)
        }
        val rawDifference = abs(smoothedRssiA - smoothedRssiB)
        val strongerSignal = when {
            rawDifference < SIMILAR_THRESHOLD_DB -> StrongerSignal.SIMILAR
            smoothedRssiA > smoothedRssiB -> StrongerSignal.DEVICE_A
            else -> StrongerSignal.DEVICE_B
        }
        return SignalComparisonResult(
            differenceDb = rawDifference.roundToInt(),
            strongerSignal = strongerSignal,
        )
    }
}

data class ComparedDeviceSignalState(
    val device: BleDeviceInfo? = null,
    val currentRssi: Int? = null,
    val smoothedRssi: Double? = null,
    val samples: List<RssiSample> = emptyList(),
    val smoothedSamples: List<SmoothedRssiSample> = emptyList(),
    val statistics: RssiStatistics? = null,
    val stability: SignalStabilityResult = SignalStabilityCalculator.calculate(emptyList()),
    val lastSeen: Long? = null,
    val isSignalStale: Boolean = false,
    val isSignalLost: Boolean = false,
)

data class CompareDevicesState(
    val deviceA: ComparedDeviceSignalState = ComparedDeviceSignalState(),
    val deviceB: ComparedDeviceSignalState = ComparedDeviceSignalState(),
    val differenceDb: Int? = null,
    val strongerSignal: StrongerSignal = StrongerSignal.UNAVAILABLE,
    val isTracking: Boolean = false,
    val trackingStartedAt: Long? = null,
    val graphTimeMillis: Long = 0L,
    val unavailableReason: TrackingUnavailableReason? = null,
)

object CompareDevicesSession {
    fun selectDeviceA(deviceA: BleDeviceInfo): CompareDevicesState = CompareDevicesState(
        deviceA = deviceA.initialComparedState(),
    )

    fun canSelectTogether(deviceA: BleDeviceInfo, deviceB: BleDeviceInfo): Boolean =
        !areSameDevice(deviceA, deviceB)

    fun start(
        deviceA: BleDeviceInfo,
        deviceB: BleDeviceInfo,
        nowMillis: Long,
        isTracking: Boolean,
        unavailableReason: TrackingUnavailableReason?,
    ): CompareDevicesState {
        require(canSelectTogether(deviceA, deviceB)) {
            "Comparison requires two different BLE devices"
        }
        return CompareDevicesState(
            deviceA = deviceA.initialComparedState(),
            deviceB = deviceB.initialComparedState(),
            differenceDb = abs(deviceA.rssi - deviceB.rssi),
            strongerSignal = SignalComparisonCalculator.calculate(
                deviceA.rssi.toDouble(),
                deviceB.rssi.toDouble(),
            ).strongerSignal,
            isTracking = isTracking,
            trackingStartedAt = nowMillis,
            graphTimeMillis = nowMillis,
            unavailableReason = unavailableReason,
        )
    }

    fun refreshAvailability(
        state: CompareDevicesState,
        nowMillis: Long,
    ): CompareDevicesState {
        val availabilityA = SignalAvailabilityCalculator.calculate(
            lastSeenMillis = state.deviceA.lastSeen,
            trackingStartedAtMillis = state.trackingStartedAt,
            nowMillis = nowMillis,
        )
        val availabilityB = SignalAvailabilityCalculator.calculate(
            lastSeenMillis = state.deviceB.lastSeen,
            trackingStartedAtMillis = state.trackingStartedAt,
            nowMillis = nowMillis,
        )
        val comparison = if (availabilityA.isLost || availabilityB.isLost) {
            SignalComparisonResult(null, StrongerSignal.UNAVAILABLE)
        } else {
            SignalComparisonCalculator.calculate(
                state.deviceA.smoothedRssi,
                state.deviceB.smoothedRssi,
            )
        }
        return state.copy(
            deviceA = state.deviceA.copy(
                isSignalStale = availabilityA.isWaiting,
                isSignalLost = availabilityA.isLost,
            ),
            deviceB = state.deviceB.copy(
                isSignalStale = availabilityB.isWaiting,
                isSignalLost = availabilityB.isLost,
            ),
            differenceDb = comparison.differenceDb,
            strongerSignal = comparison.strongerSignal,
            graphTimeMillis = nowMillis,
        )
    }

    fun matches(
        comparedDevice: ComparedDeviceSignalState,
        incomingDeviceId: String,
        incomingAddress: String?,
    ): Boolean {
        val device = comparedDevice.device ?: return false
        if (device.id == incomingDeviceId) return true
        val selectedAddress = device.address?.takeIf { it.isNotBlank() } ?: return false
        return incomingAddress?.equals(selectedAddress, ignoreCase = true) == true
    }

    private fun areSameDevice(deviceA: BleDeviceInfo, deviceB: BleDeviceInfo): Boolean {
        if (deviceA.id == deviceB.id) return true
        val addressA = deviceA.address?.takeIf { it.isNotBlank() } ?: return false
        val addressB = deviceB.address?.takeIf { it.isNotBlank() } ?: return false
        return addressA.equals(addressB, ignoreCase = true)
    }

    private fun BleDeviceInfo.initialComparedState(): ComparedDeviceSignalState =
        ComparedDeviceSignalState(
            device = this,
            currentRssi = rssi,
            smoothedRssi = rssi.toDouble(),
            lastSeen = lastSeen.takeIf { it > 0L },
        )
}
