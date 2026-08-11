package com.ble.signal.analyzer.scanner

import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerListProcessorTest {
    private val devices = listOf(
        device(id = "weak", name = "Beacon", rssi = -82, connectable = true, lastSeen = 100),
        device(id = "unknown", name = null, rssi = -45, connectable = false, lastSeen = 300),
        device(id = "strong", name = "alpha", rssi = -60, connectable = null, lastSeen = 200),
    )

    @Test
    fun allDevices_keepsEveryDeviceThatPassesMinimum() {
        val result = process(filterMode = DeviceFilterMode.All)

        assertEquals(setOf("weak", "unknown", "strong"), result.map { it.id }.toSet())
    }

    @Test
    fun namedDevicesOnly_excludesMissingAndUnknownNames() {
        val result = process(filterMode = DeviceFilterMode.Named)

        assertEquals(listOf("strong", "weak"), result.map { it.id })
        assertFalse(device(id = "x", name = "Unknown Device").hasResolvedName())
        assertFalse(device(id = "x", name = "  ").hasResolvedName())
    }

    @Test
    fun strongSignalsOnly_includesMinus70Boundary() {
        val candidates = listOf(
            device(id = "pass", rssi = -70),
            device(id = "fail", rssi = -71),
        )

        val result = ScannerListProcessor.filterAndSort(
            devices = candidates,
            filterMode = DeviceFilterMode.Strong,
            minimumRssi = -100,
            showUnnamedDevices = true,
            sortMode = DeviceSortMode.SignalStrength,
        )

        assertEquals(listOf("pass"), result.map { it.id })
    }

    @Test
    fun connectableOnly_includesOnlyExplicitTrue() {
        val result = process(filterMode = DeviceFilterMode.Connectable)

        assertEquals(listOf("weak"), result.map { it.id })
    }

    @Test
    fun minimumRssi_hidesWeakerDevices() {
        val result = process(minimumRssi = -70)

        assertEquals(listOf("unknown", "strong"), result.map { it.id })
    }

    @Test
    fun filtersCombine_namedAndMinimumRssi() {
        val result = process(
            filterMode = DeviceFilterMode.Named,
            minimumRssi = -70,
        )

        assertEquals(listOf("strong"), result.map { it.id })
    }

    @Test
    fun showUnnamedOff_appliesAlongsideSelectedMode() {
        val result = process(
            filterMode = DeviceFilterMode.Strong,
            showUnnamedDevices = false,
        )

        assertEquals(listOf("strong"), result.map { it.id })
    }

    @Test
    fun signalSort_isStrongestFirst() {
        val result = process(sortMode = DeviceSortMode.SignalStrength)

        assertEquals(listOf("unknown", "strong", "weak"), result.map { it.id })
    }

    @Test
    fun nameSort_isCaseInsensitiveAndPutsUnknownLast() {
        val candidates = listOf(
            device(id = "zulu", name = "zulu"),
            device(id = "unknown-literal", name = "Unknown Device"),
            device(id = "alpha", name = "Alpha"),
            device(id = "blank", name = " "),
            device(id = "beta", name = "beta"),
        )

        val result = ScannerListProcessor.filterAndSort(
            devices = candidates,
            filterMode = DeviceFilterMode.All,
            minimumRssi = -100,
            showUnnamedDevices = true,
            sortMode = DeviceSortMode.DeviceName,
        )

        assertEquals(
            listOf("alpha", "beta", "zulu", "blank", "unknown-literal"),
            result.map { it.id },
        )
    }

    @Test
    fun lastSeenSort_isNewestFirst() {
        val result = process(sortMode = DeviceSortMode.LastSeen)

        assertEquals(listOf("unknown", "strong", "weak"), result.map { it.id })
    }

    @Test
    fun freeze_preservesExistingOrderUpdatesValuesAndAppendsNewMatches() {
        val updated = listOf(
            device(id = "second", rssi = -40, lastSeen = 400),
            device(id = "first", rssi = -85, lastSeen = 500),
            device(id = "new-match", rssi = -55, lastSeen = 600),
            device(id = "new-filtered", rssi = -95, lastSeen = 700),
        )

        val frozen = ScannerListProcessor.frozenVisibleDevices(
            devices = updated,
            frozenDeviceIds = listOf("first", "second"),
            filterMode = DeviceFilterMode.All,
            minimumRssi = -70,
            showUnnamedDevices = true,
        )

        assertEquals(listOf("first", "second", "new-match"), frozen.deviceIds)
        assertEquals(-85, frozen.devices.first().rssi)
        assertEquals(500, frozen.devices.first().lastSeen)
    }

    @Test
    fun activeFilterCount_countsOnlyNonDefaultConstraints() {
        assertEquals(
            0,
            ScannerListProcessor.activeFilterCount(DeviceFilterMode.All, -100, true),
        )
        assertEquals(
            3,
            ScannerListProcessor.activeFilterCount(DeviceFilterMode.Named, -70, false),
        )
        assertTrue(device(id = "named", name = " Name ").hasResolvedName())
    }

    private fun process(
        filterMode: DeviceFilterMode = DeviceFilterMode.All,
        minimumRssi: Int = -100,
        showUnnamedDevices: Boolean = true,
        sortMode: DeviceSortMode = DeviceSortMode.SignalStrength,
    ): List<BleDeviceInfo> = ScannerListProcessor.filterAndSort(
        devices = devices,
        filterMode = filterMode,
        minimumRssi = minimumRssi,
        showUnnamedDevices = showUnnamedDevices,
        sortMode = sortMode,
    )

    private fun device(
        id: String,
        name: String? = id,
        rssi: Int = -60,
        connectable: Boolean? = true,
        lastSeen: Long = 0,
    ) = BleDeviceInfo(
        id = id,
        name = name,
        address = null,
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
