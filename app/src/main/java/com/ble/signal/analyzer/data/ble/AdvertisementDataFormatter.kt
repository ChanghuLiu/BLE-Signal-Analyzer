package com.ble.signal.analyzer.data.ble

import com.ble.signal.analyzer.model.BleManufacturerDataEntry
import com.ble.signal.analyzer.model.BleServiceDataEntry
import java.util.Locale

data class FormattedManufacturerDataEntry(
    val manufacturerId: String,
    val data: String?,
)

data class FormattedServiceDataEntry(
    val serviceUuid: String,
    val data: String?,
)

/** Pure display formatting for advertisement bytes. No proprietary payload decoding is done. */
object AdvertisementDataFormatter {
    fun formatHex(bytes: ByteArray?): String? = bytes
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(separator = " ") { byte ->
            String.format(Locale.ROOT, "%02X", byte.toInt() and 0xFF)
        }

    fun formatFlags(flags: Int?): String? = flags?.let {
        String.format(Locale.ROOT, "0x%02X", it and 0xFF)
    }

    fun formatManufacturerEntries(
        entries: List<BleManufacturerDataEntry>,
    ): List<FormattedManufacturerDataEntry> = entries.map { entry ->
        FormattedManufacturerDataEntry(
            manufacturerId = BleManufacturerLookup.formatId(entry.manufacturerId)
                ?: error("Manufacturer ID is required"),
            data = formatHex(entry.data),
        )
    }

    fun formatServiceDataEntries(
        entries: List<BleServiceDataEntry>,
    ): List<FormattedServiceDataEntry> = entries.map { entry ->
        FormattedServiceDataEntry(
            serviceUuid = BleServiceUuidFormatter.formatForDisplay(entry.serviceUuid)
                ?: entry.serviceUuid,
            data = formatHex(entry.data),
        )
    }
}
