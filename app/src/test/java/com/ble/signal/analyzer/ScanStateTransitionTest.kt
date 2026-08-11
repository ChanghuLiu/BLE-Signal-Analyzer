package com.ble.signal.analyzer

import com.ble.signal.analyzer.data.ble.BleScanErrorMapper
import com.ble.signal.analyzer.data.ble.BleScanErrorKind
import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanStateTransitionTest {
    @Test
    fun scanFailure_resetsRunningFlagsAndRetainsResults() {
        val device = device("existing")
        val failed = AppUiState(
            devices = listOf(device),
            visibleDevices = listOf(device),
            isScanStarting = true,
            isScanning = true,
            hasCompletedScan = true,
        ).afterScanFailure(BleScanErrorMapper.scannerUnavailable())

        assertFalse(failed.isScanStarting)
        assertFalse(failed.isScanning)
        assertFalse(failed.hasCompletedScan)
        assertEquals(listOf(device), failed.devices)
        assertEquals(listOf(device), failed.visibleDevices)
        assertEquals(BleScanErrorKind.ScannerUnavailable, failed.scanError)
    }

    @Test
    fun bluetoothOffFailure_updatesBluetoothState() {
        val failed = AppUiState(
            bluetoothEnabled = true,
            isScanning = true,
        ).afterScanFailure(BleScanErrorMapper.bluetoothDisabled())

        assertFalse(failed.bluetoothEnabled)
        assertFalse(failed.isScanning)
        assertEquals(BleScanErrorKind.BluetoothDisabled, failed.scanError)
    }

    private fun device(id: String) = BleDeviceInfo(
        id = id,
        name = "Test",
        address = null,
        rssi = -60,
        manufacturerId = null,
        manufacturerName = null,
        manufacturerData = null,
        serviceUuids = emptyList(),
        txPower = null,
        isConnectable = true,
        lastSeen = 0,
    )
}
