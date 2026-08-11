package com.ble.signal.analyzer

import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothPermissionStateResolverTest {
    @Test
    fun grantedPermissions_mapToGranted() {
        assertEquals(
            BluetoothPermissionState.Granted,
            BluetoothPermissionStateResolver.resolve(
                allGranted = true,
                hasRequestedBefore = true,
                allDeniedPermissionsCanShowRationale = false,
            ),
        )
    }

    @Test
    fun neverRequested_mapsToExplanationState() {
        assertEquals(
            BluetoothPermissionState.NotRequested,
            BluetoothPermissionStateResolver.resolve(
                allGranted = false,
                hasRequestedBefore = false,
                allDeniedPermissionsCanShowRationale = false,
            ),
        )
    }

    @Test
    fun deniedButPromptable_mapsToDenied() {
        assertEquals(
            BluetoothPermissionState.Denied,
            BluetoothPermissionStateResolver.resolve(
                allGranted = false,
                hasRequestedBefore = true,
                allDeniedPermissionsCanShowRationale = true,
            ),
        )
    }

    @Test
    fun deniedWithoutRationale_mapsToPermanentlyDenied() {
        assertEquals(
            BluetoothPermissionState.PermanentlyDenied,
            BluetoothPermissionStateResolver.resolve(
                allGranted = false,
                hasRequestedBefore = true,
                allDeniedPermissionsCanShowRationale = false,
            ),
        )
    }
}
