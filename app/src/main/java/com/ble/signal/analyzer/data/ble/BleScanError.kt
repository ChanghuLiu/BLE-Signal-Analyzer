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
)

sealed interface BleScanStartResult {
    data object Started : BleScanStartResult

    data class Failed(val error: BleScanError) : BleScanStartResult
}

object BleScanErrorMapper {
    fun fromPlatformCode(errorCode: Int): BleScanError = when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> error(BleScanErrorKind.AlreadyRunning)

        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
            error(BleScanErrorKind.ApplicationRegistrationFailed)

        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> error(BleScanErrorKind.InternalError)

        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
            error(BleScanErrorKind.FeatureUnsupported)

        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
            error(BleScanErrorKind.HardwareResourcesUnavailable)

        ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ->
            error(BleScanErrorKind.ScanningTooFrequently)

        else -> error(BleScanErrorKind.Unknown)
    }

    fun permissionRequired() = error(BleScanErrorKind.PermissionRequired)

    fun bluetoothDisabled() = error(BleScanErrorKind.BluetoothDisabled)

    fun scannerUnavailable() = error(BleScanErrorKind.ScannerUnavailable)

    private fun error(kind: BleScanErrorKind) = BleScanError(kind = kind)
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
