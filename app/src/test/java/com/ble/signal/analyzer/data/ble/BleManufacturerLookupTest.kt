package com.ble.signal.analyzer.data.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleManufacturerLookupTest {
    @Test
    fun formatId_usesFourDigitUppercaseHex() {
        assertEquals("0x004C", BleManufacturerLookup.formatId(0x004C))
        assertEquals("0x038F", BleManufacturerLookup.formatId(0x038F))
    }

    @Test
    fun nameFor_returnsVerifiedCommonManufacturers() {
        assertEquals("Apple", BleManufacturerLookup.nameFor(0x004C))
        assertEquals("Samsung", BleManufacturerLookup.nameFor(0x0075))
        assertEquals("Google", BleManufacturerLookup.nameFor(0x00E0))
        assertEquals("Nordic Semiconductor", BleManufacturerLookup.nameFor(0x0059))
        assertEquals("Fitbit", BleManufacturerLookup.nameFor(0x018E))
        assertEquals("Huawei", BleManufacturerLookup.nameFor(0x027D))
        assertEquals("Xiaomi", BleManufacturerLookup.nameFor(0x038F))
    }

    @Test
    fun unknownAndMissingManufacturers_returnNullForUiFallback() {
        assertNull(BleManufacturerLookup.nameFor(0xFFFF))
        assertNull(BleManufacturerLookup.formatId(null))
    }
}
