package com.ble.signal.analyzer

object BluetoothPermissionStateResolver {
    fun resolve(
        allGranted: Boolean,
        hasRequestedBefore: Boolean,
        allDeniedPermissionsCanShowRationale: Boolean,
    ): BluetoothPermissionState = when {
        allGranted -> BluetoothPermissionState.Granted
        !hasRequestedBefore -> BluetoothPermissionState.NotRequested
        allDeniedPermissionsCanShowRationale -> BluetoothPermissionState.Denied
        else -> BluetoothPermissionState.PermanentlyDenied
    }
}
