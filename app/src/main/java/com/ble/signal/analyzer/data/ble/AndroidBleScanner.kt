package com.ble.signal.analyzer.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.util.size
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.BleManufacturerDataEntry
import com.ble.signal.analyzer.model.BleServiceDataEntry
import java.util.Locale

class AndroidBleScanner(context: Context) {
    private val applicationContext = context.applicationContext
    private val bluetoothManager =
        applicationContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter
        get() = bluetoothManager?.adapter

    private var activeCallback: ScanCallback? = null
    private val scanSessionGuard = ScanSessionGuard()

    fun isBleSupported(): Boolean =
        applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothAdapter != null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean = runCatching {
        bluetoothAdapter?.isEnabled == true
    }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun startScan(
        onDeviceFound: (BleDeviceInfo) -> Unit,
        onFailure: (BleScanError) -> Unit,
    ): BleScanStartResult {
        if (!scanSessionGuard.tryStart()) {
            return BleScanStartResult.Failed(
                BleScanErrorMapper.fromPlatformCode(
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED,
                ),
            )
        }

        val adapter = bluetoothAdapter
            ?: return startFailure(BleScanErrorMapper.scannerUnavailable())
        val adapterEnabled = try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            return startFailure(BleScanErrorMapper.permissionRequired())
        } catch (_: RuntimeException) {
            return startFailure(BleScanErrorMapper.scannerUnavailable())
        }
        if (!adapterEnabled) {
            return startFailure(BleScanErrorMapper.bluetoothDisabled())
        }

        val scanner = try {
            adapter.bluetoothLeScanner
        } catch (_: SecurityException) {
            return startFailure(BleScanErrorMapper.permissionRequired())
        } catch (_: RuntimeException) {
            return startFailure(BleScanErrorMapper.scannerUnavailable())
        }
        if (scanner == null) {
            return startFailure(BleScanErrorMapper.scannerUnavailable())
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (activeCallback === this) {
                    result.toBleDeviceInfoOrNull()?.let(onDeviceFound)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                if (activeCallback === this) {
                    results.forEach { result ->
                        result.toBleDeviceInfoOrNull()?.let(onDeviceFound)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                if (activeCallback === this) {
                    activeCallback = null
                    scanSessionGuard.stop()
                    onFailure(BleScanErrorMapper.fromPlatformCode(errorCode))
                }
            }
        }

        activeCallback = callback
        return try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, callback)
            BleScanStartResult.Started
        } catch (_: SecurityException) {
            activeCallback = null
            startFailure(BleScanErrorMapper.permissionRequired())
        } catch (_: IllegalStateException) {
            activeCallback = null
            startFailure(BleScanErrorMapper.scannerUnavailable())
        } catch (_: RuntimeException) {
            activeCallback = null
            startFailure(BleScanErrorMapper.scannerUnavailable())
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val callback = activeCallback
        activeCallback = null
        scanSessionGuard.stop()
        if (callback == null) return
        runCatching {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
        }
    }

    fun isScanActive(): Boolean = scanSessionGuard.isActive()

    private fun startFailure(error: BleScanError): BleScanStartResult.Failed {
        activeCallback = null
        scanSessionGuard.stop()
        return BleScanStartResult.Failed(error)
    }

    private fun ScanResult.toBleDeviceInfoOrNull(): BleDeviceInfo? = runCatching {
        toBleDeviceInfo()
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun ScanResult.toBleDeviceInfo(): BleDeviceInfo {
        val record = scanRecord
        val address = runCatching { device.address }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase(Locale.ROOT)
        val advertisedName = record?.deviceName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val platformName = runCatching { device.name }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val manufacturerData = record?.manufacturerSpecificData
        val manufacturerEntryCount = runCatching { manufacturerData?.size ?: 0 }
            .getOrDefault(0)
        val manufacturerEntries = (0 until manufacturerEntryCount).mapNotNull { index ->
            runCatching {
                val id = manufacturerData?.keyAt(index) ?: return@runCatching null
                val data = manufacturerData?.get(id) ?: return@runCatching null
                BleManufacturerDataEntry(
                    manufacturerId = id,
                    data = data.copyOf(),
                )
            }.getOrNull()
        }
        val primaryManufacturerEntry = manufacturerEntries.firstOrNull()
        val manufacturerId = primaryManufacturerEntry?.manufacturerId
        val txPower = record?.txPowerLevel?.takeUnless { it == Int.MIN_VALUE }
        val normalizedServiceUuids = record?.serviceUuids
            .orEmpty()
            .mapNotNull { parcelUuid ->
                runCatching {
                    BleServiceUuidFormatter.normalize(parcelUuid.uuid.toString())
                }.getOrNull()
            }
            .distinct()
        val serviceDataEntries = record?.serviceData
            .orEmpty()
            .mapNotNull { (parcelUuid, data) ->
                val normalizedUuid = runCatching {
                    BleServiceUuidFormatter.normalize(parcelUuid.uuid.toString())
                }.getOrNull() ?: return@mapNotNull null
                BleServiceDataEntry(
                    serviceUuid = normalizedUuid,
                    data = data.copyOf(),
                )
            }
            .sortedBy(BleServiceDataEntry::serviceUuid)
        val advertisementFlags = record?.advertiseFlags?.takeUnless { it < 0 }
        val rawAdvertisementBytes = record?.bytes?.copyOf()

        return BleDeviceInfo(
            id = address ?: "device-${device.hashCode()}",
            name = advertisedName ?: platformName,
            address = address,
            rssi = rssi,
            manufacturerId = manufacturerId,
            manufacturerName = manufacturerId?.let(BleManufacturerLookup::nameFor),
            manufacturerData = primaryManufacturerEntry?.data?.copyOf(),
            serviceUuids = normalizedServiceUuids,
            txPower = txPower,
            isConnectable = isConnectable,
            lastSeen = System.currentTimeMillis(),
            manufacturerDataEntries = manufacturerEntries,
            localName = advertisedName,
            serviceDataEntries = serviceDataEntries,
            advertisementFlags = advertisementFlags,
            rawAdvertisementBytes = rawAdvertisementBytes,
        )
    }
}
