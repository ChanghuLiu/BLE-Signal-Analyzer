package com.ble.signal.analyzer.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvEscaperTest {
    @Test
    fun `plain value is unchanged`() {
        assertEquals("BLE Sensor", CsvEscaper.escape("BLE Sensor"))
    }

    @Test
    fun `comma-containing value is quoted`() {
        assertEquals("\"My Device, Office\"", CsvEscaper.escape("My Device, Office"))
    }

    @Test
    fun `quotes are doubled inside quoted value`() {
        assertEquals(
            "\"My \"\"BLE\"\" Device\"",
            CsvEscaper.escape("My \"BLE\" Device"),
        )
        assertEquals(
            "\"My \"\"BLE\"\", Device\"",
            CsvEscaper.escape("My \"BLE\", Device"),
        )
    }

    @Test
    fun `newline-containing value is quoted`() {
        assertEquals("\"first\nsecond\"", CsvEscaper.escape("first\nsecond"))
    }

    @Test
    fun `empty value remains empty`() {
        assertEquals("", CsvEscaper.escape(""))
    }

    @Test
    fun `export file names are readable and contain only safe fixed components`() {
        assertEquals(
            "BLE_Signal_Analyzer_Signal_1970-01-01_000000.csv",
            CsvExportFileName.create(CsvExportType.SIGNAL, 0L),
        )
        assertEquals(
            "BLE_Signal_Analyzer_Compare_1970-01-01_000000.csv",
            CsvExportFileName.create(CsvExportType.COMPARE, 0L),
        )
        assertEquals(
            "BLE_Signal_Analyzer_Environment_1970-01-01_000000.csv",
            CsvExportFileName.create(CsvExportType.ENVIRONMENT, 0L),
        )
    }
}
