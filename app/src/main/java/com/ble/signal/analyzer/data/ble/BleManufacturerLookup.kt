package com.ble.signal.analyzer.data.ble

import java.util.Locale

object BleManufacturerLookup {
    // Small V1 subset of Bluetooth SIG Assigned Numbers company identifiers.
    private val companyNames = mapOf(
        0x0002 to "Intel",
        0x0006 to "Microsoft",
        0x000D to "Texas Instruments",
        0x004C to "Apple",
        0x0059 to "Nordic Semiconductor",
        0x0075 to "Samsung",
        0x0087 to "Garmin",
        0x00C4 to "LG",
        0x00E0 to "Google",
        0x012D to "Sony",
        0x018E to "Fitbit",
        0x027D to "Huawei",
        0x038F to "Xiaomi",
    )

    fun nameFor(manufacturerId: Int): String? = companyNames[manufacturerId]

    fun formatId(manufacturerId: Int?): String? = manufacturerId?.let {
        String.format(Locale.ROOT, "0x%04X", it)
    }
}
