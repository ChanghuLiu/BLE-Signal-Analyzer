package com.ble.signal.analyzer.data.ble

import android.bluetooth.le.ScanCallback

enum class BleScanErrorKind {
    AlreadyRunning,
    ApplicationRegistrationFailed,
    InternalError,
    FeatureUnsupported,
    HardwareResourcesUnavailable,
    ScanningTooFrequently,
    PermissionRequired,
    BluetoothDisabled,
    ScannerUnavailable,
    Unknown,
}

data class BleScanError(
    val kind: BleScanErrorKind,
    val userMessage: String,
)

sealed interface BleScanStartResult {
    data object Started : BleScanStartResult

    data class Failed(val error: BleScanError) : BleScanStartResult
}

object BleScanErrorMapper {
    fun fromPlatformCode(errorCode: Int): BleScanError = when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> error(
            BleScanErrorKind.AlreadyRunning,
            "A Bluetooth scan is already running.",
        )

        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> error(
            BleScanErrorKind.ApplicationRegistrationFailed,
            "Bluetooth scanning could not start. Please try again.",
        )

        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> error(
            BleScanErrorKind.InternalError,
            "Bluetooth scanning encountered an internal error.",
        )

        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> error(
            BleScanErrorKind.FeatureUnsupported,
            "This Bluetooth scan mode is not supported on this device.",
        )

        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> error(
            BleScanErrorKind.HardwareResourcesUnavailable,
            "Bluetooth scanning resources are temporarily unavailable.",
        )

        ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> error(
            BleScanErrorKind.ScanningTooFrequently,
            "Bluetooth scans are starting too frequently. Wait a moment and try again.",
        )

        else -> error(
            BleScanErrorKind.Unknown,
            "Bluetooth scan failed. Please try again.",
        )
    }

    fun permissionRequired() = error(
        BleScanErrorKind.PermissionRequired,
        "Bluetooth permission is required before scanning can start.",
    )

    fun bluetoothDisabled() = error(
        BleScanErrorKind.BluetoothDisabled,
        "Bluetooth is turned off.",
    )

    fun scannerUnavailable() = error(
        BleScanErrorKind.ScannerUnavailable,
        "Bluetooth scanning is unavailable on this device.",
    )

    private fun error(kind: BleScanErrorKind, message: String) = BleScanError(
        kind = kind,
        userMessage = message,
    )
}

internal class ScanSessionGuard {
    private var active = false

    @Synchronized
    fun tryStart(): Boolean {
        if (active) return false
        active = true
        return true
    }

    @Synchronized
    fun stop() {
        active = false
    }

    @Synchronized
    fun isActive(): Boolean = active
}
