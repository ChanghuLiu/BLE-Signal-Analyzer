package com.ble.signal.analyzer.model

data class BleDeviceInfo(
    val id: String,
    val name: String?,
    val address: String?,
    val rssi: Int,
    val manufacturerId: Int?,
    val manufacturerName: String?,
    val manufacturerData: ByteArray?,
    val serviceUuids: List<String>,
    val txPower: Int?,
    val isConnectable: Boolean?,
    val lastSeen: Long,
    val manufacturerDataEntries: List<BleManufacturerDataEntry> = emptyList(),
)

data class BleManufacturerDataEntry(
    val manufacturerId: Int,
    val data: ByteArray,
)

enum class SignalQuality(val label: String) {
    Excellent("Excellent"),
    Strong("Strong"),
    Good("Good"),
    Fair("Fair"),
    Weak("Weak"),
}

fun signalQualityFor(rssi: Int): SignalQuality = when {
    rssi >= -49 -> SignalQuality.Excellent
    rssi >= -59 -> SignalQuality.Strong
    rssi >= -69 -> SignalQuality.Good
    rssi >= -79 -> SignalQuality.Fair
    else -> SignalQuality.Weak
}
