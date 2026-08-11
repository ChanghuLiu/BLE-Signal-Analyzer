package com.ble.signal.analyzer.export

import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.SignalTrackerState
import com.ble.signal.analyzer.signal.SmoothedRssiSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExportFormatterTest {
    @Test
    fun `signal export includes stable headers timestamps and multiple samples`() {
        val state = SignalTrackerState(
            trackingStartedAt = 0L,
            samples = listOf(
                RssiSample(timestamp = 0L, rssi = -61),
                RssiSample(timestamp = 789L, rssi = -58),
            ),
            smoothedSamples = listOf(
                SmoothedRssiSample(timestamp = 0L, rssi = -60.8),
                SmoothedRssiSample(timestamp = 789L, rssi = -59.9),
            ),
        )

        val document = requireNotNull(SessionExportFormatter.format(state))
        val lines = document.content.trimEnd().lines()

        assertEquals(CsvExportType.SIGNAL, document.type)
        assertEquals(
            "timestamp_iso,elapsed_ms,raw_rssi_dbm,smoothed_rssi_dbm",
            lines[0],
        )
        assertEquals("1970-01-01T00:00:00Z,0,-61,-60.8", lines[1])
        assertEquals("1970-01-01T00:00:00.789Z,789,-58,-59.9", lines[2])
        assertEquals(3, lines.size)
    }

    @Test
    fun `missing smoothed sample is exported as empty instead of invented`() {
        val state = SignalTrackerState(
            trackingStartedAt = 1_000L,
            samples = listOf(RssiSample(timestamp = 1_500L, rssi = -70)),
        )

        val content = requireNotNull(SessionExportFormatter.format(state)).content

        assertTrue(content.contains("1970-01-01T00:00:01.500Z,500,-70,\r\n"))
    }

    @Test
    fun `empty signal session is rejected`() {
        assertNull(SessionExportFormatter.format(SignalTrackerState()))
    }
}
