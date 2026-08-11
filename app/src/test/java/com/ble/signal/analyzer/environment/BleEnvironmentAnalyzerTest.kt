package com.ble.signal.analyzer.environment

import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class BleEnvironmentAnalyzerTest {
    @Test
    fun totalCountUsesUniqueDevices() {
        val summary = BleEnvironmentAnalyzer.analyze(
            listOf(
                device("a", "AA:BB", -50, lastSeen = 1L),
                device("duplicate", "aa:bb", -55, lastSeen = 2L),
                device("b", "CC:DD", -70, lastSeen = 1L),
            ),
        )

        assertEquals(2, summary.totalDevices)
    }

    @Test
    fun namedAndUnknownCountsAreCalculated() {
        val summary = BleEnvironmentAnalyzer.analyze(
            listOf(
                device("a", null, -50, name = "Sensor"),
                device("b", null, -60, name = null),
                device("c", null, -70, name = "  "),
                device("d", null, -80, name = "Unknown Device"),
            ),
        )

        assertEquals(1, summary.namedDevices)
        assertEquals(3, summary.unknownDevices)
    }

    @Test
    fun connectableCountIncludesOnlyExplicitlyConnectableDevices() {
        val summary = BleEnvironmentAnalyzer.analyze(
            listOf(
                device("a", null, -50, connectable = true),
                device("b", null, -60, connectable = false),
                device("c", null, -70, connectable = null),
            ),
        )

        assertEquals(1, summary.connectableDevices)
    }

    @Test
    fun environmentSignalGroupsUseDocumentedBoundaries() {
        val summary = BleEnvironmentAnalyzer.analyze(
            listOf(
                device("strong-a", null, -30),
                device("strong-b", null, -60),
                device("medium-a", null, -61),
                device("medium-b", null, -79),
                device("weak", null, -80),
            ),
        )

        assertEquals(2, summary.strongDevices)
        assertEquals(2, summary.mediumDevices)
        assertEquals(1, summary.weakDevices)
    }

    @Test
    fun fiveLevelDistributionUsesSignalQualityBoundaries() {
        val distribution = BleEnvironmentAnalyzer.analyze(
            listOf(
                device("excellent", null, -49),
                device("strong", null, -50),
                device("good", null, -60),
                device("fair", null, -70),
                device("weak", null, -80),
            ),
        ).signalDistribution

        assertEquals(1, distribution.excellent)
        assertEquals(1, distribution.strong)
        assertEquals(1, distribution.good)
        assertEquals(1, distribution.fair)
        assertEquals(1, distribution.weak)
    }

    @Test
    fun activityIsLowForZeroThroughFiveDevices() {
        assertEquals(BleActivityLevel.LOW, BleEnvironmentAnalyzer.activityLevel(0))
        assertEquals(BleActivityLevel.LOW, BleEnvironmentAnalyzer.activityLevel(5))
    }

    @Test
    fun activityIsModerateForSixThroughTwentyDevices() {
        assertEquals(BleActivityLevel.MODERATE, BleEnvironmentAnalyzer.activityLevel(6))
        assertEquals(BleActivityLevel.MODERATE, BleEnvironmentAnalyzer.activityLevel(20))
    }

    @Test
    fun activityIsHighForTwentyOneOrMoreDevices() {
        assertEquals(BleActivityLevel.HIGH, BleEnvironmentAnalyzer.activityLevel(21))
        assertEquals(BleActivityLevel.HIGH, BleEnvironmentAnalyzer.activityLevel(100))
    }

    private fun device(
        id: String,
        address: String?,
        rssi: Int,
        name: String? = id,
        connectable: Boolean? = null,
        lastSeen: Long = 0L,
    ): BleDeviceInfo = BleDeviceInfo(
        id = id,
        name = name,
        address = address,
        rssi = rssi,
        manufacturerId = null,
        manufacturerName = null,
        manufacturerData = null,
        serviceUuids = emptyList(),
        txPower = null,
        isConnectable = connectable,
        lastSeen = lastSeen,
    )
}
