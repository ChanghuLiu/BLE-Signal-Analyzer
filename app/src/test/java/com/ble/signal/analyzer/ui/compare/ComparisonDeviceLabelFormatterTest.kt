package com.ble.signal.analyzer.ui.compare

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonDeviceLabelFormatterTest {
    @Test
    fun meaningfulNameIsAppendedToRole() {
        assertEquals(
            "Device A — AirPods",
            formatComparisonDeviceLabel("Device A", "AirPods"),
        )
    }

    @Test
    fun nullNameUsesOnlyRole() {
        assertEquals("Device A", formatComparisonDeviceLabel("Device A", null))
    }

    @Test
    fun blankNameUsesOnlyRole() {
        assertEquals("Device A", formatComparisonDeviceLabel("Device A", ""))
    }

    @Test
    fun unknownDeviceNameUsesOnlyRole() {
        assertEquals("Device A", formatComparisonDeviceLabel("Device A", "Unknown Device"))
    }

    @Test
    fun roleNameUsesOnlyRole() {
        assertEquals("Device A", formatComparisonDeviceLabel("Device A", "Device A"))
    }
}
