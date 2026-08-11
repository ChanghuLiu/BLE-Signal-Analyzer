package com.ble.signal.analyzer.data.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleServiceUuidFormatterTest {
    @Test
    fun normalize_expandsStandard16BitUuid() {
        assertEquals(
            "0000180D-0000-1000-8000-00805F9B34FB",
            BleServiceUuidFormatter.normalize("0x180d"),
        )
    }

    @Test
    fun normalize_uppercasesFullUuid() {
        assertEquals(
            "0000180F-0000-1000-8000-00805F9B34FB",
            BleServiceUuidFormatter.normalize(
                "0000180f-0000-1000-8000-00805f9b34fb",
            ),
        )
    }

    @Test
    fun knownStandardService_hasShortIdAndName() {
        val uuid = "0000180D-0000-1000-8000-00805F9B34FB"

        assertEquals(0x180D, BleServiceUuidFormatter.standardServiceId(uuid))
        assertEquals("Heart Rate", BleServiceUuidFormatter.serviceNameFor(uuid))
        assertEquals(
            "0x180D — Heart Rate\n$uuid",
            BleServiceUuidFormatter.formatForDisplay(uuid),
        )
    }

    @Test
    fun unknownStandardService_keepsNormalizedUuidWithoutGuessingName() {
        assertEquals(
            "0xFEAA\n0000FEAA-0000-1000-8000-00805F9B34FB",
            BleServiceUuidFormatter.formatForDisplay("FEAA"),
        )
        assertNull(BleServiceUuidFormatter.serviceNameFor("FEAA"))
    }

    @Test
    fun missingAndMalformedValues_areHandledSafely() {
        assertNull(BleServiceUuidFormatter.formatListForDisplay(emptyList()))
        assertNull(BleServiceUuidFormatter.formatForDisplay("  "))
        assertEquals("malformed", BleServiceUuidFormatter.formatForDisplay(" malformed "))
        assertNull(BleServiceUuidFormatter.normalize("malformed"))
    }
}
