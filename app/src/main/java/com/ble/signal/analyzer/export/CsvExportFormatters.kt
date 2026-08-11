package com.ble.signal.analyzer.export

import com.ble.signal.analyzer.environment.BleEnvironmentAnalyzer
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.signal.CompareDevicesState
import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.SignalTrackerState
import com.ble.signal.analyzer.signal.SmoothedRssiSample
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.Locale

enum class CsvExportType(val fileLabel: String) {
    SIGNAL("Signal"),
    COMPARE("Compare"),
    ENVIRONMENT("Environment"),
}

data class CsvExportDocument(
    val type: CsvExportType,
    val content: String,
)

object CsvExportFileName {
    private val timestampFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd_HHmmss", Locale.ROOT)
        .withZone(ZoneOffset.UTC)

    fun create(type: CsvExportType, nowMillis: Long): String =
        "BLE_Signal_Analyzer_${type.fileLabel}_${timestampFormatter.format(Instant.ofEpochMilli(nowMillis))}.csv"
}

object SessionExportFormatter {
    private val headers = listOf(
        "timestamp_iso",
        "elapsed_ms",
        "raw_rssi_dbm",
        "smoothed_rssi_dbm",
    )

    fun format(state: SignalTrackerState): CsvExportDocument? {
        if (state.samples.isEmpty()) return null
        val sessionStart = state.trackingStartedAt ?: state.samples.minOf(RssiSample::timestamp)
        val smoothedByTimestamp = state.smoothedSamples.toTimestampQueues()
        val rows = state.samples
            .sortedBy(RssiSample::timestamp)
            .map { sample ->
                listOf(
                    sample.timestamp.toIsoTimestamp(),
                    (sample.timestamp - sessionStart).coerceAtLeast(0L).toString(),
                    sample.rssi.toString(),
                    smoothedByTimestamp.poll(sample.timestamp)?.toCsvDecimal().orEmpty(),
                )
            }
        return CsvExportDocument(
            type = CsvExportType.SIGNAL,
            content = CsvEscaper.document(headers, rows),
        )
    }
}

object ComparisonExportFormatter {
    private val headers = listOf(
        "timestamp_iso",
        "elapsed_ms",
        "device_role",
        "device_name",
        "raw_rssi_dbm",
        "smoothed_rssi_dbm",
    )

    fun format(state: CompareDevicesState): CsvExportDocument? {
        val samplesA = state.deviceA.samples
        val samplesB = state.deviceB.samples
        if (samplesA.isEmpty() && samplesB.isEmpty()) return null

        val earliestSample = (samplesA.asSequence() + samplesB.asSequence())
            .minOf(RssiSample::timestamp)
        val sessionStart = state.trackingStartedAt ?: earliestSample
        val rows = buildList {
            addDeviceRows(
                role = "Device A",
                name = state.deviceA.device?.name,
                rawSamples = samplesA,
                smoothedSamples = state.deviceA.smoothedSamples,
                sessionStart = sessionStart,
            )
            addDeviceRows(
                role = "Device B",
                name = state.deviceB.device?.name,
                rawSamples = samplesB,
                smoothedSamples = state.deviceB.smoothedSamples,
                sessionStart = sessionStart,
            )
        }.sortedWith(compareBy<ComparisonRow>({ it.timestamp }, { it.role }))

        return CsvExportDocument(
            type = CsvExportType.COMPARE,
            content = CsvEscaper.document(headers, rows.map(ComparisonRow::values)),
        )
    }

    private fun MutableList<ComparisonRow>.addDeviceRows(
        role: String,
        name: String?,
        rawSamples: List<RssiSample>,
        smoothedSamples: List<SmoothedRssiSample>,
        sessionStart: Long,
    ) {
        val smoothedByTimestamp = smoothedSamples.toTimestampQueues()
        rawSamples.forEach { sample ->
            add(
                ComparisonRow(
                    timestamp = sample.timestamp,
                    role = role,
                    values = listOf(
                        sample.timestamp.toIsoTimestamp(),
                        (sample.timestamp - sessionStart).coerceAtLeast(0L).toString(),
                        role,
                        name?.trim().orEmpty(),
                        sample.rssi.toString(),
                        smoothedByTimestamp.poll(sample.timestamp)?.toCsvDecimal().orEmpty(),
                    ),
                ),
            )
        }
    }

    private data class ComparisonRow(
        val timestamp: Long,
        val role: String,
        val values: List<String>,
    )
}

object EnvironmentExportFormatter {
    private val headers = listOf(
        "device_name",
        "manufacturer",
        "device_address",
        "rssi_dbm",
        "signal_quality",
        "connectable",
        "last_seen",
        "manufacturer_id",
        "service_uuids",
    )

    fun format(devices: List<BleDeviceInfo>): CsvExportDocument? {
        val uniqueDevices = BleEnvironmentAnalyzer.uniqueDevices(devices)
        if (uniqueDevices.isEmpty()) return null
        val rows = uniqueDevices.map { device ->
            listOf(
                device.name?.trim().orEmpty(),
                device.manufacturerName?.trim().orEmpty(),
                device.address?.trim().orEmpty(),
                device.rssi.toString(),
                signalQualityFor(device.rssi).name.uppercase(Locale.ROOT),
                device.isConnectable?.toString().orEmpty(),
                device.lastSeen.takeIf { it > 0L }?.toIsoTimestamp().orEmpty(),
                device.manufacturerId?.let(::formatManufacturerId).orEmpty(),
                device.serviceUuids.joinToString(";"),
            )
        }
        return CsvExportDocument(
            type = CsvExportType.ENVIRONMENT,
            content = CsvEscaper.document(headers, rows),
        )
    }

    private fun formatManufacturerId(manufacturerId: Int): String =
        String.format(Locale.ROOT, "0x%04X", manufacturerId)
}

private fun List<SmoothedRssiSample>.toTimestampQueues(): MutableMap<Long, ArrayDeque<Double>> {
    val values = mutableMapOf<Long, ArrayDeque<Double>>()
    forEach { sample ->
        values.getOrPut(sample.timestamp, ::ArrayDeque).addLast(sample.rssi)
    }
    return values
}

private fun MutableMap<Long, ArrayDeque<Double>>.poll(timestamp: Long): Double? =
    get(timestamp)?.pollFirst()

private fun Long.toIsoTimestamp(): String = Instant.ofEpochMilli(this).toString()

private fun Double.toCsvDecimal(): String = String.format(Locale.ROOT, "%.1f", this)
