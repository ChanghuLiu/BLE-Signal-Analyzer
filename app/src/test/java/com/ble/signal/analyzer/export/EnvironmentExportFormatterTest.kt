package com.ble.signal.analyzer.export

import com.ble.signal.analyzer.model.BleDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentExportFormatterTest {
    @Test
    fun `environment export uses one latest row per unique device`() {
        val older = device(
            id = "old",
            name = "Old name",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -80,
            lastSeen = 1_000L,
        )
        val latest = device(
            id = "new",
            name = "Office, Sensor",
            address = "aa:bb:cc:dd:ee:01",
            rssi = -58,
            lastSeen = 2_000L,
            manufacturerName = "Acme",
            manufacturerId = 0x004C,
            connectable = true,
            serviceUuids = listOf("0x180F", "0xFEAA"),
        )

        val document = requireNotNull(EnvironmentExportFormatter.format(listOf(older, latest)))
        val lines = document.content.trimEnd().lines()

        assertEquals(CsvExportType.ENVIRONMENT, document.type)
        assertEquals(
            "device_name,manufacturer,device_address,rssi_dbm,signal_quality,connectable,last_seen,manufacturer_id,service_uuids",
            lines[0],
        )
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("\"Office, Sensor\",Acme,aa:bb:cc:dd:ee:01,-58,STRONG,true,"))
        assertTrue(lines[1].contains(",0x004C,0x180F;0xFEAA"))
    }

    @Test
    fun `environment unavailable fields remain empty`() {
        val content = requireNotNull(
            EnvironmentExportFormatter.format(
                listOf(device("one", null, null, -82, 0L)),
            ),
        ).content

        assertTrue(content.contains(",,,-82,WEAK,,,,\r\n"))
    }

    @Test
    fun `empty environment is rejected`() {
        assertNull(EnvironmentExportFormatter.format(emptyList()))
    }

    private fun device(
        id: String,
        name: String?,
        address: String?,
        rssi: Int,
        lastSeen: Long,
        manufacturerName: String? = null,
        manufacturerId: Int? = null,
        connectable: Boolean? = null,
        serviceUuids: List<String> = emptyList(),
    ) = BleDeviceInfo(
        id = id,
        name = name,
        address = address,
        rssi = rssi,
        manufacturerId = manufacturerId,
        manufacturerName = manufacturerName,
        manufacturerData = null,
        serviceUuids = serviceUuids,
        txPower = null,
        isConnectable = connectable,
        lastSeen = lastSeen,
    )
}
