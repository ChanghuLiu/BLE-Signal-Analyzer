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
    val localName: String? = null,
    val serviceDataEntries: List<BleServiceDataEntry> = emptyList(),
    val advertisementFlags: Int? = null,
    val rawAdvertisementBytes: ByteArray? = null,
)

data class BleManufacturerDataEntry(
    val manufacturerId: Int,
    val data: ByteArray,
)

data class BleServiceDataEntry(
    val serviceUuid: String,
    val data: ByteArray,
)

enum class SignalQuality {
    Excellent,
    Strong,
    Good,
    Fair,
    Weak,
}

fun signalQualityFor(rssi: Int): SignalQuality = when {
    rssi >= -49 -> SignalQuality.Excellent
    rssi >= -59 -> SignalQuality.Strong
    rssi >= -69 -> SignalQuality.Good
    rssi >= -79 -> SignalQuality.Fair
    else -> SignalQuality.Weak
}
