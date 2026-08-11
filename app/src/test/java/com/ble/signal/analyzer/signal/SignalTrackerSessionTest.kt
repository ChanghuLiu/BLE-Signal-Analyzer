package com.ble.signal.analyzer.signal

import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalTrackerSessionTest {
    private val recentLastSeen = 1_000_000L

    @Test
    fun trackerStartsWithSelectedDeviceSnapshotAndIsNotLost() {
        val state = startState(lastSeen = recentLastSeen)
        val availability = SignalTrackerSession.availability(
            state = state,
            nowMillis = recentLastSeen,
        )

        assertEquals("selected-id", state.deviceId)
        assertEquals("AA:BB:CC:DD:EE:FF", state.deviceAddress)
        assertEquals(-36, state.currentRssi)
        assertEquals(recentLastSeen, state.lastSeen)
        assertFalse(availability.isWaiting)
        assertFalse(availability.isLost)
    }

    @Test
    fun threeSecondsWithoutUpdateIsNotLostOrWaiting() {
        val availability = availabilityAfter(3_000L)

        assertFalse(availability.isWaiting)
        assertFalse(availability.isLost)
    }

    @Test
    fun sixSecondsWithoutUpdateIsWaitingButNotLost() {
        val availability = availabilityAfter(6_000L)

        assertTrue(availability.isWaiting)
        assertFalse(availability.isLost)
    }

    @Test
    fun sixteenSecondsWithoutUpdateIsLost() {
        val availability = availabilityAfter(16_000L)

        assertTrue(availability.isWaiting)
        assertTrue(availability.isLost)
    }

    @Test
    fun matchingDeviceAdvertisementResetsLostState() {
        val lostState = startState(lastSeen = recentLastSeen).copy(
            isSignalStale = true,
            isSignalLost = true,
        )
        val refreshed = SignalTrackerSession.onSelectedAdvertisement(
            state = lostState,
            incomingDeviceId = "selected-id",
            incomingAddress = "AA:BB:CC:DD:EE:FF",
            timestampMillis = recentLastSeen + 16_000L,
        )

        assertEquals(recentLastSeen + 16_000L, refreshed.lastSeen)
        assertFalse(refreshed.isSignalStale)
        assertFalse(refreshed.isSignalLost)
    }

    @Test
    fun unrelatedDeviceAdvertisementDoesNotResetSelectedDeviceTimer() {
        val lostState = startState(lastSeen = recentLastSeen).copy(
            isSignalStale = true,
            isSignalLost = true,
        )
        val unchanged = SignalTrackerSession.onSelectedAdvertisement(
            state = lostState,
            incomingDeviceId = "unrelated-id",
            incomingAddress = "11:22:33:44:55:66",
            timestampMillis = recentLastSeen + 16_000L,
        )

        assertEquals(lostState, unchanged)
    }

    private fun availabilityAfter(elapsedMillis: Long): SignalAvailability =
        SignalTrackerSession.availability(
            state = startState(lastSeen = recentLastSeen),
            nowMillis = recentLastSeen + elapsedMillis,
        )

    private fun startState(lastSeen: Long): SignalTrackerState = SignalTrackerSession.start(
        device = BleDeviceInfo(
            id = "selected-id",
            name = "Test Device",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -36,
            manufacturerId = null,
            manufacturerName = null,
            manufacturerData = null,
            serviceUuids = emptyList(),
            txPower = null,
            isConnectable = true,
            lastSeen = lastSeen,
        ),
        nowMillis = lastSeen,
        isTracking = true,
        unavailableReason = null,
    )
}
