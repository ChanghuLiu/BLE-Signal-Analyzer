package com.ble.signal.analyzer.ui.compare

private val DEFAULT_GENERIC_DEVICE_NAMES = setOf(
    "Unknown Device",
    "Device A",
    "Device B",
)

/** Formats a comparison role and only appends a meaningful BLE device name. */
internal fun formatComparisonDeviceLabel(
    roleLabel: String,
    deviceName: String?,
    localizedGenericNames: Collection<String> = emptyList(),
): String {
    val normalizedName = deviceName?.trim().orEmpty()
    if (normalizedName.isEmpty()) return roleLabel

    val isGenericName = (DEFAULT_GENERIC_DEVICE_NAMES + localizedGenericNames + roleLabel)
        .any { genericName -> normalizedName.equals(genericName.trim(), ignoreCase = true) }
    return if (isGenericName) roleLabel else "$roleLabel — $normalizedName"
}
