package com.ble.signal.analyzer.scanner

import com.ble.signal.analyzer.model.BleDeviceInfo
import java.util.Locale

enum class DeviceFilterMode(val displayName: String) {
    All("All devices"),
    Named("Named devices only"),
    Strong("Strong signals only"),
    Connectable("Connectable only"),
}

enum class DeviceSortMode(val displayName: String, val shortName: String) {
    SignalStrength("Signal strength", "Signal"),
    DeviceName("Device name", "Name"),
    LastSeen("Last seen", "Recent"),
}

data class FrozenDeviceList(
    val devices: List<BleDeviceInfo>,
    val deviceIds: List<String>,
)

/** Pure list transformations used by the scanner ViewModel and unit tests. */
object ScannerListProcessor {
    const val DEFAULT_MINIMUM_RSSI = -100
    const val STRONG_SIGNAL_MINIMUM_RSSI = -70

    fun filterAndSort(
        devices: List<BleDeviceInfo>,
        filterMode: DeviceFilterMode,
        minimumRssi: Int,
        showUnnamedDevices: Boolean,
        sortMode: DeviceSortMode,
    ): List<BleDeviceInfo> = devices
        .filter { device ->
            matchesFilter(
                device = device,
                filterMode = filterMode,
                minimumRssi = minimumRssi,
                showUnnamedDevices = showUnnamedDevices,
            )
        }
        .sortedWith(comparatorFor(sortMode))

    fun matchesFilter(
        device: BleDeviceInfo,
        filterMode: DeviceFilterMode,
        minimumRssi: Int,
        showUnnamedDevices: Boolean,
    ): Boolean {
        if (device.rssi < minimumRssi.coerceIn(-100, -30)) return false
        if (!showUnnamedDevices && !device.hasResolvedName()) return false

        return when (filterMode) {
            DeviceFilterMode.All -> true
            DeviceFilterMode.Named -> device.hasResolvedName()
            DeviceFilterMode.Strong -> device.rssi >= STRONG_SIGNAL_MINIMUM_RSSI
            DeviceFilterMode.Connectable -> device.isConnectable == true
        }
    }

    /**
     * While frozen, devices that were visible when Freeze was enabled remain in place even
     * if a changing RSSI no longer passes the active filter. Newly matching devices are appended.
     * Unfreezing or starting a new scan reapplies the current filter and sort from scratch.
     */
    fun frozenVisibleDevices(
        devices: List<BleDeviceInfo>,
        frozenDeviceIds: List<String>,
        filterMode: DeviceFilterMode,
        minimumRssi: Int,
        showUnnamedDevices: Boolean,
    ): FrozenDeviceList {
        val devicesById = devices.associateBy(BleDeviceInfo::id)
        val retainedIds = frozenDeviceIds.filter(devicesById::containsKey)
        val retainedIdSet = retainedIds.toHashSet()
        val newMatchingIds = devices.asSequence()
            .filter { it.id !in retainedIdSet }
            .filter {
                matchesFilter(
                    device = it,
                    filterMode = filterMode,
                    minimumRssi = minimumRssi,
                    showUnnamedDevices = showUnnamedDevices,
                )
            }
            .map(BleDeviceInfo::id)
            .toList()
        val updatedIds = retainedIds + newMatchingIds

        return FrozenDeviceList(
            devices = updatedIds.mapNotNull(devicesById::get),
            deviceIds = updatedIds,
        )
    }

    fun activeFilterCount(
        filterMode: DeviceFilterMode,
        minimumRssi: Int,
        showUnnamedDevices: Boolean,
    ): Int = listOf(
        filterMode != DeviceFilterMode.All,
        minimumRssi != DEFAULT_MINIMUM_RSSI,
        !showUnnamedDevices,
    ).count { it }

    private fun comparatorFor(sortMode: DeviceSortMode): Comparator<BleDeviceInfo> =
        when (sortMode) {
            DeviceSortMode.SignalStrength ->
                compareByDescending<BleDeviceInfo> { it.rssi }
                    .thenBy { it.normalizedName() }
                    .thenBy { it.id }

            DeviceSortMode.DeviceName ->
                compareBy<BleDeviceInfo> { !it.hasResolvedName() }
                    .thenBy { it.normalizedName() }
                    .thenBy { it.id }

            DeviceSortMode.LastSeen ->
                compareByDescending<BleDeviceInfo> { it.lastSeen }
                    .thenByDescending { it.rssi }
                    .thenBy { it.id }
        }
}

fun BleDeviceInfo.hasResolvedName(): Boolean {
    val value = name?.trim().orEmpty()
    return value.isNotEmpty() && !value.equals("Unknown Device", ignoreCase = true)
}

private fun BleDeviceInfo.normalizedName(): String =
    name?.trim()?.lowercase(Locale.ROOT).orEmpty()
