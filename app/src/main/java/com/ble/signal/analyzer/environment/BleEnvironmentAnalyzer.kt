package com.ble.signal.analyzer.environment

import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.SignalQuality
import com.ble.signal.analyzer.model.signalQualityFor
import java.util.Locale

enum class BleActivityLevel {
    LOW,
    MODERATE,
    HIGH,
}

data class SignalDistribution(
    val excellent: Int = 0,
    val strong: Int = 0,
    val good: Int = 0,
    val fair: Int = 0,
    val weak: Int = 0,
)

data class BleEnvironmentSummary(
    val totalDevices: Int = 0,
    val namedDevices: Int = 0,
    val unknownDevices: Int = 0,
    val connectableDevices: Int = 0,
    val strongDevices: Int = 0,
    val mediumDevices: Int = 0,
    val weakDevices: Int = 0,
    val signalDistribution: SignalDistribution = SignalDistribution(),
    val activityLevel: BleActivityLevel = BleActivityLevel.LOW,
)

object BleEnvironmentAnalyzer {
    /**
     * App-defined environment grouping: strong >= -60 dBm, medium -61..-79 dBm,
     * and weak <= -80 dBm. This is separate from the five-level signal quality scale.
     */
    fun analyze(devices: List<BleDeviceInfo>): BleEnvironmentSummary {
        val uniqueDevices = uniqueDevices(devices)
        val namedDevices = uniqueDevices.count { it.hasMeaningfulName() }

        return BleEnvironmentSummary(
            totalDevices = uniqueDevices.size,
            namedDevices = namedDevices,
            unknownDevices = uniqueDevices.size - namedDevices,
            connectableDevices = uniqueDevices.count { it.isConnectable == true },
            strongDevices = uniqueDevices.count { it.rssi >= -60 },
            mediumDevices = uniqueDevices.count { it.rssi in -79..-61 },
            weakDevices = uniqueDevices.count { it.rssi <= -80 },
            signalDistribution = SignalDistribution(
                excellent = uniqueDevices.count {
                    signalQualityFor(it.rssi) == SignalQuality.Excellent
                },
                strong = uniqueDevices.count {
                    signalQualityFor(it.rssi) == SignalQuality.Strong
                },
                good = uniqueDevices.count {
                    signalQualityFor(it.rssi) == SignalQuality.Good
                },
                fair = uniqueDevices.count {
                    signalQualityFor(it.rssi) == SignalQuality.Fair
                },
                weak = uniqueDevices.count {
                    signalQualityFor(it.rssi) == SignalQuality.Weak
                },
            ),
            activityLevel = activityLevel(uniqueDevices.size),
        )
    }

    /** App-defined BLE Activity thresholds based only on unique devices in this scan session. */
    fun activityLevel(deviceCount: Int): BleActivityLevel = when {
        deviceCount <= 5 -> BleActivityLevel.LOW
        deviceCount <= 20 -> BleActivityLevel.MODERATE
        else -> BleActivityLevel.HIGH
    }

    /** Returns the latest result for each address (or scanner ID when no address is available). */
    fun uniqueDevices(devices: List<BleDeviceInfo>): List<BleDeviceInfo> = devices
        .groupBy(::uniqueDeviceKey)
        .values
        .map { matches -> matches.maxBy(BleDeviceInfo::lastSeen) }
        .sortedBy(::uniqueDeviceKey)

    private fun uniqueDeviceKey(device: BleDeviceInfo): String = device.address
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.uppercase(Locale.ROOT)
        ?: device.id

    private fun BleDeviceInfo.hasMeaningfulName(): Boolean {
        val normalized = name?.trim().orEmpty()
        return normalized.isNotEmpty() &&
            !normalized.equals("Unknown Device", ignoreCase = true)
    }
}
