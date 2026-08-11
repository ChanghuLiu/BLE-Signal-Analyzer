package com.ble.signal.analyzer.export

import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.signal.CompareDevicesState
import com.ble.signal.analyzer.signal.ComparedDeviceSignalState
import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.SmoothedRssiSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonExportFormatterTest {
    @Test
    fun `comparison export contains normalized independently timestamped role rows`() {
        val state = CompareDevicesState(
            deviceA = ComparedDeviceSignalState(
                device = device("A", "AirPods"),
                samples = listOf(RssiSample(1_000L, -48)),
                smoothedSamples = listOf(SmoothedRssiSample(1_000L, -49.2)),
            ),
            deviceB = ComparedDeviceSignalState(
                device = device("B", "BLE Sensor"),
                samples = listOf(RssiSample(1_750L, -67)),
                smoothedSamples = listOf(SmoothedRssiSample(1_750L, -66.1)),
            ),
            trackingStartedAt = 1_000L,
        )

        val document = requireNotNull(ComparisonExportFormatter.format(state))
        val lines = document.content.trimEnd().lines()

        assertEquals(CsvExportType.COMPARE, document.type)
        assertEquals(
            "timestamp_iso,elapsed_ms,device_role,device_name,raw_rssi_dbm,smoothed_rssi_dbm",
            lines[0],
        )
        assertTrue(lines[1].contains(",0,Device A,AirPods,-48,-49.2"))
        assertTrue(lines[2].contains(",750,Device B,BLE Sensor,-67,-66.1"))
        assertEquals(3, lines.size)
    }

    @Test
    fun `missing device sample does not create interpolated comparison row`() {
        val state = CompareDevicesState(
            deviceA = ComparedDeviceSignalState(
                device = device("A", "Only A"),
                samples = listOf(RssiSample(2_000L, -55)),
                smoothedSamples = listOf(SmoothedRssiSample(2_000L, -54.5)),
            ),
            deviceB = ComparedDeviceSignalState(device = device("B", "No sample")),
        )

        val lines = requireNotNull(ComparisonExportFormatter.format(state))
            .content
            .trimEnd()
            .lines()

        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("Device A"))
        assertFalse(lines[1].contains("Device B"))
    }

    @Test
    fun `duplicate names remain distinguishable by role`() {
        val state = CompareDevicesState(
            deviceA = ComparedDeviceSignalState(
                device = device("A", "Sensor"),
                samples = listOf(RssiSample(3_000L, -50)),
            ),
            deviceB = ComparedDeviceSignalState(
                device = device("B", "Sensor"),
                samples = listOf(RssiSample(3_100L, -60)),
            ),
        )

        val content = requireNotNull(ComparisonExportFormatter.format(state)).content

        assertTrue(content.contains("Device A,Sensor"))
        assertTrue(content.contains("Device B,Sensor"))
    }

    @Test
    fun `empty comparison is rejected`() {
        assertNull(ComparisonExportFormatter.format(CompareDevicesState()))
    }

    private fun device(id: String, name: String) = BleDeviceInfo(
        id = id,
        name = name,
        address = "00:00:00:00:00:$id",
        rssi = -60,
        manufacturerId = null,
        manufacturerName = null,
        manufacturerData = null,
        serviceUuids = emptyList(),
        txPower = null,
        isConnectable = null,
        lastSeen = 0L,
    )
}
