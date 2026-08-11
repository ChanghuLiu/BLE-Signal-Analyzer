package com.ble.signal.analyzer.data.ble

import com.ble.signal.analyzer.model.BleManufacturerDataEntry
import com.ble.signal.analyzer.model.BleServiceDataEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvertisementDataFormatterTest {
    @Test
    fun hexFormattingUsesUnsignedUppercaseBytes() {
        assertEquals(
            "02 01 1A FF",
            AdvertisementDataFormatter.formatHex(byteArrayOf(0x02, 0x01, 0x1A, 0xFF.toByte())),
        )
    }

    @Test
    fun multipleManufacturerEntriesAreFormattedSeparately() {
        val result = AdvertisementDataFormatter.formatManufacturerEntries(
            listOf(
                BleManufacturerDataEntry(0x004C, byteArrayOf(0x07, 0x19)),
                BleManufacturerDataEntry(0x0075, byteArrayOf(0x01, 0x02)),
            ),
        )

        assertEquals(2, result.size)
        assertEquals("0x004C", result[0].manufacturerId)
        assertEquals("07 19", result[0].data)
        assertEquals("0x0075", result[1].manufacturerId)
        assertEquals("01 02", result[1].data)
    }

    @Test
    fun missingManufacturerDataProducesNoEntries() {
        assertTrue(AdvertisementDataFormatter.formatManufacturerEntries(emptyList()).isEmpty())
    }

    @Test
    fun serviceDataIsFormattedWithUuidAndHex() {
        val result = AdvertisementDataFormatter.formatServiceDataEntries(
            listOf(BleServiceDataEntry("FEAA", byteArrayOf(0x10, 0xEE.toByte(), 0x00))),
        ).single()

        assertTrue(result.serviceUuid.startsWith("0xFEAA"))
        assertEquals("10 EE 00", result.data)
    }

    @Test
    fun missingServiceDataProducesNoEntries() {
        assertTrue(AdvertisementDataFormatter.formatServiceDataEntries(emptyList()).isEmpty())
    }

    @Test
    fun rawAdvertisementUsesTheSameHexFormatting() {
        assertEquals(
            "02 01 1A 1A FF 4C 00",
            AdvertisementDataFormatter.formatHex(
                byteArrayOf(0x02, 0x01, 0x1A, 0x1A, 0xFF.toByte(), 0x4C, 0x00),
            ),
        )
    }

    @Test
    fun flagsUseTwoDigitHex() {
        assertEquals("0x1A", AdvertisementDataFormatter.formatFlags(0x1A))
    }

    @Test
    fun emptyRawDataIsUnavailable() {
        assertNull(AdvertisementDataFormatter.formatHex(byteArrayOf()))
        assertNull(AdvertisementDataFormatter.formatHex(null))
    }
}
