package com.ble.signal.analyzer.data.ble

import java.util.Locale
import java.util.UUID

object BleServiceUuidFormatter {
    private const val BLUETOOTH_BASE_SUFFIX = "-0000-1000-8000-00805F9B34FB"

    private val standardServiceNames = mapOf(
        0x1800 to "Generic Access",
        0x1801 to "Generic Attribute",
        0x180A to "Device Information",
        0x180D to "Heart Rate",
        0x180F to "Battery Service",
        0x1812 to "Human Interface Device",
    )

    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        val withoutPrefix = trimmed.removePrefix("0x").removePrefix("0X")
        val expanded = when {
            withoutPrefix.matches(Regex("[0-9A-Fa-f]{4}")) ->
                "0000$withoutPrefix$BLUETOOTH_BASE_SUFFIX"

            withoutPrefix.matches(Regex("[0-9A-Fa-f]{8}")) ->
                "$withoutPrefix$BLUETOOTH_BASE_SUFFIX"

            else -> trimmed
        }

        return runCatching {
            UUID.fromString(expanded).toString().uppercase(Locale.ROOT)
        }.getOrNull()
    }

    fun standardServiceId(value: String): Int? {
        val normalized = normalize(value) ?: return null
        if (!normalized.endsWith(BLUETOOTH_BASE_SUFFIX)) return null
        if (!normalized.startsWith("0000")) return null
        return normalized.substring(4, 8).toIntOrNull(radix = 16)
    }

    fun serviceNameFor(value: String): String? =
        standardServiceId(value)?.let(standardServiceNames::get)

    fun formatForDisplay(value: String): String {
        val normalized = normalize(value)
            ?: return value.trim().takeIf { it.isNotEmpty() } ?: "Not available"
        val serviceId = standardServiceId(normalized) ?: return normalized
        val shortValue = String.format(Locale.ROOT, "0x%04X", serviceId)
        val serviceName = standardServiceNames[serviceId]
        val summary = if (serviceName == null) shortValue else "$shortValue — $serviceName"
        return "$summary\n$normalized"
    }

    fun formatListForDisplay(values: List<String>): String {
        val formatted = values
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map(::formatForDisplay)
        return formatted.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
            ?: "Not available"
    }
}
