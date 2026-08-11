package com.ble.signal.analyzer.signal

import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareDevicesTest {
    @Test
    fun differenceCalculationUsesAbsoluteDbDifference() {
        val result = SignalComparisonCalculator.calculate(-45.0, -65.0)

        assertEquals(20, result.differenceDb)
        assertEquals(StrongerSignal.DEVICE_A, result.strongerSignal)
    }

    @Test
    fun signalsWithinThreeDbAreSimilar() {
        val result = SignalComparisonCalculator.calculate(-55.0, -57.0)

        assertEquals(2, result.differenceDb)
        assertEquals(StrongerSignal.SIMILAR, result.strongerSignal)
    }

    @Test
    fun missingSignalMakesComparisonUnavailable() {
        val result = SignalComparisonCalculator.calculate(-55.0, null)

        assertNull(result.differenceDb)
        assertEquals(StrongerSignal.UNAVAILABLE, result.strongerSignal)
    }

    @Test
    fun sameDeviceCannotBeSelectedForBothSlots() {
        val deviceA = device("same-id", "AA:BB:CC:DD:EE:FF", -50, 1_000L)
        val duplicate = device("other-id", "aa:bb:cc:dd:ee:ff", -60, 1_000L)

        assertFalse(CompareDevicesSession.canSelectTogether(deviceA, deviceA))
        assertFalse(CompareDevicesSession.canSelectTogether(deviceA, duplicate))
    }

    @Test
    fun twoDifferentDevicesCanBeSelected() {
        assertTrue(
            CompareDevicesSession.canSelectTogether(
                device("a", "AA:AA:AA:AA:AA:AA", -50, 1_000L),
                device("b", "BB:BB:BB:BB:BB:BB", -60, 1_000L),
            ),
        )
    }

    @Test
    fun deviceAvailabilityUsesIndependentLastSeenTimes() {
        val state = CompareDevicesSession.start(
            deviceA = device("a", "AA:AA:AA:AA:AA:AA", -50, 1_000L),
            deviceB = device("b", "BB:BB:BB:BB:BB:BB", -60, 15_000L),
            nowMillis = 1_000L,
            isTracking = true,
            unavailableReason = null,
        )

        val refreshed = CompareDevicesSession.refreshAvailability(state, nowMillis = 17_000L)

        assertTrue(refreshed.deviceA.isSignalLost)
        assertFalse(refreshed.deviceB.isSignalStale)
        assertFalse(refreshed.deviceB.isSignalLost)
    }

    @Test
    fun deviceADisappearanceDoesNotMarkDeviceBLost() {
        val state = CompareDevicesSession.start(
            deviceA = device("a", null, -50, 1_000L),
            deviceB = device("b", null, -60, 16_000L),
            nowMillis = 1_000L,
            isTracking = true,
            unavailableReason = null,
        )

        val refreshed = CompareDevicesSession.refreshAvailability(state, nowMillis = 17_000L)

        assertTrue(refreshed.deviceA.isSignalLost)
        assertFalse(refreshed.deviceB.isSignalLost)
    }

    private fun device(
        id: String,
        address: String?,
        rssi: Int,
        lastSeen: Long,
    ): BleDeviceInfo = BleDeviceInfo(
        id = id,
        name = id,
        address = address,
        rssi = rssi,
        manufacturerId = null,
        manufacturerName = null,
        manufacturerData = null,
        serviceUuids = emptyList(),
        txPower = null,
        isConnectable = null,
        lastSeen = lastSeen,
    )
}
