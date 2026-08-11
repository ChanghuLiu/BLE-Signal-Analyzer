package com.ble.signal.analyzer.data.ble

import android.bluetooth.le.ScanCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleScanErrorTest {
    @Test
    fun commonPlatformCodes_haveFriendlyMessages() {
        val cases = mapOf(
            ScanCallback.SCAN_FAILED_ALREADY_STARTED to BleScanErrorKind.AlreadyRunning,
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED to
                BleScanErrorKind.ApplicationRegistrationFailed,
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR to BleScanErrorKind.InternalError,
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED to
                BleScanErrorKind.FeatureUnsupported,
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES to
                BleScanErrorKind.HardwareResourcesUnavailable,
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY to
                BleScanErrorKind.ScanningTooFrequently,
        )

        cases.forEach { (code, expectedKind) ->
            val error = BleScanErrorMapper.fromPlatformCode(code)
            assertEquals(expectedKind, error.kind)
            assertFalse(error.userMessage.contains(code.toString()))
        }
    }

    @Test
    fun unknownPlatformCode_usesGenericRecoveryMessage() {
        val error = BleScanErrorMapper.fromPlatformCode(Int.MAX_VALUE)

        assertEquals(BleScanErrorKind.Unknown, error.kind)
        assertEquals("Bluetooth scan failed. Please try again.", error.userMessage)
    }

    @Test
    fun sessionGuard_preventsDuplicateStartsAndStopIsIdempotent() {
        val guard = ScanSessionGuard()

        assertTrue(guard.tryStart())
        assertFalse(guard.tryStart())
        assertTrue(guard.isActive())
        guard.stop()
        guard.stop()
        assertFalse(guard.isActive())
        assertTrue(guard.tryStart())
    }
}
