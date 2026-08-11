package com.ble.signal.analyzer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ble.signal.analyzer.data.ble.AndroidBleScanner
import com.ble.signal.analyzer.data.ble.BleScanError
import com.ble.signal.analyzer.data.ble.BleScanErrorKind
import com.ble.signal.analyzer.data.ble.BleScanStartResult
import com.ble.signal.analyzer.data.settings.AppSettings
import com.ble.signal.analyzer.data.settings.SettingsRepository
import com.ble.signal.analyzer.data.settings.SettingsValueNormalizer
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.scanner.DeviceFilterMode
import com.ble.signal.analyzer.scanner.DeviceSortMode
import com.ble.signal.analyzer.scanner.ScannerListProcessor
import com.ble.signal.analyzer.signal.ProximityLabelMapper
import com.ble.signal.analyzer.signal.CompareDevicesSession
import com.ble.signal.analyzer.signal.CompareDevicesState
import com.ble.signal.analyzer.signal.ComparedDevice
import com.ble.signal.analyzer.signal.ProximityAlertEvaluationState
import com.ble.signal.analyzer.signal.ProximityAlertEvaluator
import com.ble.signal.analyzer.signal.ProximityAlertStatus
import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.RssiSampleWindow
import com.ble.signal.analyzer.signal.RssiSmoother
import com.ble.signal.analyzer.signal.SignalStatisticsAccumulator
import com.ble.signal.analyzer.signal.SignalComparisonCalculator
import com.ble.signal.analyzer.signal.SignalComparisonResult
import com.ble.signal.analyzer.signal.SignalStabilityCalculator
import com.ble.signal.analyzer.signal.SignalTrackerConfig
import com.ble.signal.analyzer.signal.SignalTrackerSession
import com.ble.signal.analyzer.signal.SignalTrackerState
import com.ble.signal.analyzer.signal.SignalTrendCalculator
import com.ble.signal.analyzer.signal.SmoothedRssiSample
import com.ble.signal.analyzer.signal.StrongerSignal
import com.ble.signal.analyzer.signal.TrackingUnavailableReason
import com.ble.signal.analyzer.signal.toTrackingUnavailableReason
import com.ble.signal.analyzer.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    Scanner,
    DeviceDetail,
    SignalTracker,
    CompareSelection,
    CompareDevices,
    AdvertisementInspector,
    BleEnvironment,
    Settings,
    PrivacyPolicy,
    HowBleSignalsWork,
    About,
}

enum class BluetoothPermissionState {
    NotRequested,
    Granted,
    Denied,
    PermanentlyDenied,
}

data class AppUiState(
    val destination: AppDestination = AppDestination.Scanner,
    val informationBackStack: List<AppDestination> = emptyList(),
    /** Complete in-memory results for the current scan session. */
    val devices: List<BleDeviceInfo> = emptyList(),
    /** Filtered/sorted projection consumed by the Scanner UI. */
    val visibleDevices: List<BleDeviceInfo> = emptyList(),
    val selectedDevice: BleDeviceInfo? = null,
    val signalTrackerState: SignalTrackerState = SignalTrackerState(),
    val compareDevicesState: CompareDevicesState = CompareDevicesState(),
    val compareSelectionError: Boolean = false,
    val isAdvertisementInspectorRefreshing: Boolean = false,
    val advertisementInspectorTimeMillis: Long = 0L,
    val advertisementInspectorError: BleScanErrorKind? = null,
    val isScanStarting: Boolean = false,
    val isScanning: Boolean = false,
    val hasCompletedScan: Boolean = false,
    val scanError: BleScanErrorKind? = null,
    val bleSupported: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val permissionState: BluetoothPermissionState = BluetoothPermissionState.NotRequested,
    val permissionPromptDismissed: Boolean = false,
    val filterMode: DeviceFilterMode = DeviceFilterMode.All,
    val sortMode: DeviceSortMode = DeviceSortMode.SignalStrength,
    val freezeEnabled: Boolean = false,
    val frozenDeviceIds: List<String> = emptyList(),
    val scanDurationSeconds: Int = 30,
    val showUnnamedDevices: Boolean = true,
    val minimumRssi: Int = -100,
    val keepScreenAwake: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val signalDescriptions: Boolean = true,
    val proximityAlertThreshold: Int = AppSettings.DEFAULT_PROXIMITY_ALERT_THRESHOLD,
) {
    val activeFilterCount: Int
        get() = ScannerListProcessor.activeFilterCount(
            filterMode = filterMode,
            minimumRssi = minimumRssi,
            showUnnamedDevices = showUnnamedDevices,
        )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val bleScanner = AndroidBleScanner(application)
    private val settingsRepository = SettingsRepository(application)
    private val mutableUiState = MutableStateFlow(AppUiState())
    private var scanTimeoutJob: Job? = null
    private var trackerStatusJob: Job? = null
    private var comparisonStatusJob: Job? = null
    private var advertisementInspectorStatusJob: Job? = null
    private val trackerStatistics = SignalStatisticsAccumulator()
    private val comparisonStatisticsA = SignalStatisticsAccumulator()
    private val comparisonStatisticsB = SignalStatisticsAccumulator()
    private var proximityAlertEvaluationState = ProximityAlertEvaluationState()
    private var nextVibrationEventId = 0L

    val uiState = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                mutableUiState.update { state ->
                    state.copy(
                        scanDurationSeconds = settings.scanDurationSeconds,
                        showUnnamedDevices = settings.showUnnamedDevices,
                        minimumRssi = settings.minimumRssi,
                        keepScreenAwake = settings.keepScreenAwake,
                        themeMode = settings.themeMode,
                        signalDescriptions = settings.signalDescriptions,
                        proximityAlertThreshold = settings.proximityAlertThreshold,
                    ).refreshVisibleDevices()
                }
            }
        }
    }

    fun isBleSupportedOnDevice(): Boolean = bleScanner.isBleSupported()

    fun isBluetoothEnabledOnDevice(): Boolean = bleScanner.isBluetoothEnabled()

    fun showScanError(kind: BleScanErrorKind) {
        mutableUiState.update { it.copy(scanError = kind) }
    }

    fun updateBluetoothEnvironment(
        bleSupported: Boolean,
        bluetoothEnabled: Boolean,
        permissionState: BluetoothPermissionState,
        permissionRequestCompleted: Boolean = false,
    ) {
        val currentState = mutableUiState.value
        val environmentUnavailable = !bleSupported || !bluetoothEnabled ||
            permissionState != BluetoothPermissionState.Granted
        val mustStop = (currentState.isScanning || currentState.isScanStarting) &&
            (!bleSupported || !bluetoothEnabled ||
                permissionState != BluetoothPermissionState.Granted)
        if (mustStop) {
            stopScanning(markCompleted = false)
        }
        if (currentState.signalTrackerState.isTracking && environmentUnavailable) {
            val reason = when {
                !bleSupported -> TrackingUnavailableReason.BLE_UNSUPPORTED
                permissionState != BluetoothPermissionState.Granted ->
                    TrackingUnavailableReason.PERMISSION_LOST

                else -> TrackingUnavailableReason.BLUETOOTH_DISABLED
            }
            stopSignalTrackingForUnavailable(reason)
        }
        if (currentState.compareDevicesState.isTracking && environmentUnavailable) {
            val reason = when {
                !bleSupported -> TrackingUnavailableReason.BLE_UNSUPPORTED
                permissionState != BluetoothPermissionState.Granted ->
                    TrackingUnavailableReason.PERMISSION_LOST

                else -> TrackingUnavailableReason.BLUETOOTH_DISABLED
            }
            stopComparisonForUnavailable(reason)
        }
        if (currentState.isAdvertisementInspectorRefreshing && environmentUnavailable) {
            stopAdvertisementInspection(
                error = when {
                    !bleSupported -> BleScanErrorKind.FeatureUnsupported
                    permissionState != BluetoothPermissionState.Granted ->
                        BleScanErrorKind.PermissionRequired

                    else -> BleScanErrorKind.BluetoothDisabled
                },
            )
        }

        mutableUiState.update { state ->
            state.copy(
                bleSupported = bleSupported,
                bluetoothEnabled = bluetoothEnabled,
                permissionState = permissionState,
                permissionPromptDismissed = if (permissionRequestCompleted) {
                    false
                } else {
                    state.permissionPromptDismissed
                },
            )
        }
    }

    fun deferPermissionRequest() {
        mutableUiState.update { it.copy(permissionPromptDismissed = true) }
    }

    fun toggleScanning() {
        val state = mutableUiState.value
        if (state.isScanStarting) return
        if (state.isScanning) {
            stopScanning(markCompleted = true)
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        val state = mutableUiState.value
        if (!state.bleSupported) {
            mutableUiState.update {
                it.copy(scanError = BleScanErrorKind.FeatureUnsupported)
            }
            return
        }
        if (state.permissionState != BluetoothPermissionState.Granted) {
            mutableUiState.update {
                it.copy(scanError = BleScanErrorKind.PermissionRequired)
            }
            return
        }
        if (!state.bluetoothEnabled) {
            mutableUiState.update {
                it.copy(scanError = BleScanErrorKind.BluetoothDisabled)
            }
            return
        }

        scanTimeoutJob?.cancel()
        mutableUiState.update {
            it.copy(
                isScanStarting = true,
                isScanning = true,
                hasCompletedScan = false,
                scanError = null,
            )
        }

        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleDeviceFound,
            onFailure = ::handleScanFailure,
        )) {
            BleScanStartResult.Started -> mutableUiState.update { current ->
                current.copy(
                    devices = emptyList(),
                    visibleDevices = emptyList(),
                    selectedDevice = null,
                    isScanStarting = false,
                    freezeEnabled = false,
                    frozenDeviceIds = emptyList(),
                ).refreshVisibleDevices()
            }

            is BleScanStartResult.Failed -> {
                handleScanFailure(result.error)
                return
            }
        }

        val durationMillis = mutableUiState.value.scanDurationSeconds * 1_000L
        scanTimeoutJob = viewModelScope.launch {
            delay(durationMillis)
            stopScanning(markCompleted = true)
        }
    }

    private fun handleDeviceFound(incoming: BleDeviceInfo) {
        mutableUiState.update { state ->
            if (!state.isScanning) return@update state

            val existingIndex = state.devices.indexOfFirst { it.id == incoming.id }
            val existing = state.devices.getOrNull(existingIndex)
            val merged = existing?.mergeLatest(incoming) ?: incoming
            val updatedDevices = if (existingIndex >= 0) {
                state.devices.toMutableList().apply { set(existingIndex, merged) }
            } else {
                state.devices + merged
            }
            state.copy(
                devices = updatedDevices,
                selectedDevice = state.selectedDevice?.let { selected ->
                    if (selected.id == merged.id) merged else selected
                },
            ).refreshVisibleDevices()
        }
    }

    private fun handleScanFailure(error: BleScanError) {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        bleScanner.stopScan()
        mutableUiState.update { it.afterScanFailure(error) }
    }

    private fun stopScanning(markCompleted: Boolean) {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        bleScanner.stopScan()
        mutableUiState.update { state ->
            if (!state.isScanning && !state.isScanStarting) state
            else state.copy(
                isScanStarting = false,
                isScanning = false,
                hasCompletedScan = markCompleted,
            )
        }
    }

    private fun startSignalTracking(device: BleDeviceInfo) {
        stopScanning(markCompleted = false)
        releaseSignalTrackingScanner()
        trackerStatistics.reset()
        proximityAlertEvaluationState = ProximityAlertEvaluationState()

        val currentState = mutableUiState.value
        val unavailableReason = when {
            !currentState.bleSupported ->
                TrackingUnavailableReason.BLE_UNSUPPORTED

            currentState.permissionState != BluetoothPermissionState.Granted ->
                TrackingUnavailableReason.PERMISSION_REQUIRED

            !currentState.bluetoothEnabled -> TrackingUnavailableReason.BLUETOOTH_DISABLED
            else -> null
        }
        val now = System.currentTimeMillis()
        mutableUiState.update {
            it.copy(
                destination = AppDestination.SignalTracker,
                signalTrackerState = SignalTrackerSession.start(
                    device = device,
                    nowMillis = now,
                    isTracking = unavailableReason == null,
                    unavailableReason = unavailableReason,
                ),
            )
        }
        if (unavailableReason != null) return

        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleTrackingDeviceFound,
            onFailure = ::handleTrackingFailure,
        )) {
            BleScanStartResult.Started -> startTrackerStatusTicker()
            is BleScanStartResult.Failed -> {
                handleTrackingFailure(result.error)
                return
            }
        }
    }

    private fun handleTrackingDeviceFound(incoming: BleDeviceInfo) {
        val currentTracker = mutableUiState.value.signalTrackerState
        if (
            !currentTracker.isTracking ||
            !SignalTrackerSession.matchesSelectedDevice(
                state = currentTracker,
                incomingDeviceId = incoming.id,
                incomingAddress = incoming.address,
            )
        ) return

        val statistics = trackerStatistics.add(incoming.rssi)
        val smoothedRssi = RssiSmoother.next(
            previousSmoothedRssi = currentTracker.smoothedRssi,
            currentRssi = incoming.rssi,
        )
        val alertEvaluation = ProximityAlertEvaluator.evaluate(
            state = proximityAlertEvaluationState,
            enabled = currentTracker.proximityAlertEnabled,
            smoothedRssi = smoothedRssi,
            alertThreshold = mutableUiState.value.proximityAlertThreshold,
            timestampMillis = incoming.lastSeen,
        )
        proximityAlertEvaluationState = alertEvaluation.state
        val vibrationEventId = if (alertEvaluation.shouldVibrate) {
            ++nextVibrationEventId
        } else {
            null
        }
        mutableUiState.update { state ->
            val tracker = state.signalTrackerState
            if (
                !tracker.isTracking ||
                !SignalTrackerSession.matchesSelectedDevice(
                    state = tracker,
                    incomingDeviceId = incoming.id,
                    incomingAddress = incoming.address,
                )
            ) {
                return@update state
            }

            val timestamp = incoming.lastSeen
            val trackerWithFreshSignal = SignalTrackerSession.onSelectedAdvertisement(
                state = tracker,
                incomingDeviceId = incoming.id,
                incomingAddress = incoming.address,
                timestampMillis = timestamp,
            )
            val samples = RssiSampleWindow.retainRecent(
                samples = trackerWithFreshSignal.samples + RssiSample(timestamp, incoming.rssi),
                nowMillis = timestamp,
            )
            val smoothedSamples = RssiSampleWindow.retainRecentSmoothed(
                samples = trackerWithFreshSignal.smoothedSamples +
                    SmoothedRssiSample(timestamp, smoothedRssi),
                nowMillis = timestamp,
            )
            val stability = SignalStabilityCalculator.calculate(
                samples = samples,
                smoothedSamples = smoothedSamples,
                nowMillis = timestamp,
            )
            val trend = SignalTrendCalculator.calculate(
                samples = smoothedSamples,
                nowMillis = timestamp,
            )
            val updatedDevice = state.selectedDevice?.let { selected ->
                if (selected.id == incoming.id) selected.mergeLatest(incoming) else selected
            } ?: incoming

            state.copy(
                selectedDevice = updatedDevice,
                signalTrackerState = trackerWithFreshSignal.copy(
                    deviceName = incoming.name?.takeIf { it.isNotBlank() }
                        ?: trackerWithFreshSignal.deviceName,
                    currentRssi = incoming.rssi,
                    smoothedRssi = smoothedRssi,
                    samples = samples,
                    smoothedSamples = smoothedSamples,
                    minRssi = statistics.min,
                    maxRssi = statistics.max,
                    averageRssi = statistics.average,
                    stability = stability,
                    trend = trend,
                    proximityLabel = ProximityLabelMapper.fromSmoothedRssi(smoothedRssi),
                    graphTimeMillis = timestamp,
                    unavailableReason = null,
                    proximityAlertStatus = alertEvaluation.state.status,
                    pendingVibrationEventId = vibrationEventId
                        ?: trackerWithFreshSignal.pendingVibrationEventId,
                ),
            )
        }
    }

    private fun handleTrackingFailure(error: BleScanError) {
        releaseSignalTrackingScanner()
        mutableUiState.update { state ->
            state.copy(
                signalTrackerState = state.signalTrackerState.copy(
                    isTracking = false,
                    unavailableReason = error.kind.toTrackingUnavailableReason(),
                ),
                bluetoothEnabled = if (error.kind == BleScanErrorKind.BluetoothDisabled) {
                    false
                } else {
                    state.bluetoothEnabled
                },
            )
        }
    }

    private fun startTrackerStatusTicker() {
        trackerStatusJob?.cancel()
        trackerStatusJob = viewModelScope.launch {
            while (true) {
                delay(SignalTrackerConfig.STATUS_TICK_MILLIS)
                val now = System.currentTimeMillis()
                mutableUiState.update { state ->
                    val tracker = state.signalTrackerState
                    if (!tracker.isTracking) return@update state

                    val availability = SignalTrackerSession.availability(
                        state = tracker,
                        nowMillis = now,
                    )
                    val samples = RssiSampleWindow.retainRecent(
                        samples = tracker.samples,
                        nowMillis = now,
                    )
                    val smoothedSamples = RssiSampleWindow.retainRecentSmoothed(
                        samples = tracker.smoothedSamples,
                        nowMillis = now,
                    )
                    state.copy(
                        signalTrackerState = tracker.copy(
                            samples = samples,
                            smoothedSamples = smoothedSamples,
                            stability = SignalStabilityCalculator.calculate(
                                samples = samples,
                                smoothedSamples = smoothedSamples,
                                nowMillis = now,
                            ),
                            isSignalStale = availability.isWaiting,
                            isSignalLost = availability.isLost,
                            graphTimeMillis = now,
                        ),
                    )
                }
            }
        }
    }

    private fun stopSignalTrackingForUnavailable(reason: TrackingUnavailableReason) {
        releaseSignalTrackingScanner()
        mutableUiState.update { state ->
            state.copy(
                signalTrackerState = state.signalTrackerState.copy(
                    isTracking = false,
                    graphTimeMillis = System.currentTimeMillis(),
                    unavailableReason = reason,
                    pendingVibrationEventId = null,
                ),
            )
        }
    }

    private fun stopSignalTrackingForNavigation() {
        releaseSignalTrackingScanner()
        mutableUiState.update { state ->
            state.copy(
                signalTrackerState = state.signalTrackerState.copy(
                    isTracking = false,
                    unavailableReason = null,
                    pendingVibrationEventId = null,
                ),
            )
        }
    }

    private fun releaseSignalTrackingScanner() {
        trackerStatusJob?.cancel()
        trackerStatusJob = null
        bleScanner.stopScan()
    }

    fun openCompareSelection() {
        val deviceA = mutableUiState.value.selectedDevice ?: return
        stopScanning(markCompleted = false)
        releaseComparisonScanner()
        mutableUiState.update { state ->
            state.copy(
                destination = AppDestination.CompareSelection,
                compareDevicesState = CompareDevicesSession.selectDeviceA(deviceA),
                compareSelectionError = false,
            )
        }
    }

    fun selectComparisonDevice(deviceB: BleDeviceInfo) {
        val deviceA = mutableUiState.value.compareDevicesState.deviceA.device ?: return
        if (!CompareDevicesSession.canSelectTogether(deviceA, deviceB)) {
            mutableUiState.update { it.copy(compareSelectionError = true) }
            return
        }
        startComparison(deviceA, deviceB)
    }

    private fun startComparison(deviceA: BleDeviceInfo, deviceB: BleDeviceInfo) {
        stopScanning(markCompleted = false)
        releaseSignalTrackingScanner()
        releaseComparisonScanner()
        comparisonStatisticsA.reset()
        comparisonStatisticsB.reset()

        val currentState = mutableUiState.value
        val unavailableReason = trackingUnavailableReason(currentState)
        val now = System.currentTimeMillis()
        mutableUiState.update { state ->
            state.copy(
                destination = AppDestination.CompareDevices,
                compareDevicesState = CompareDevicesSession.start(
                    deviceA = deviceA,
                    deviceB = deviceB,
                    nowMillis = now,
                    isTracking = unavailableReason == null,
                    unavailableReason = unavailableReason,
                ),
                compareSelectionError = false,
            )
        }
        if (unavailableReason != null) return

        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleComparisonDeviceFound,
            onFailure = ::handleComparisonFailure,
        )) {
            BleScanStartResult.Started -> startComparisonStatusTicker()
            is BleScanStartResult.Failed -> handleComparisonFailure(result.error)
        }
    }

    private fun handleComparisonDeviceFound(incoming: BleDeviceInfo) {
        val comparisonSnapshot = mutableUiState.value.compareDevicesState
        val comparedDevice = when {
            CompareDevicesSession.matches(
                comparisonSnapshot.deviceA,
                incoming.id,
                incoming.address,
            ) -> ComparedDevice.DEVICE_A

            CompareDevicesSession.matches(
                comparisonSnapshot.deviceB,
                incoming.id,
                incoming.address,
            ) -> ComparedDevice.DEVICE_B

            else -> return
        }
        val statistics = when (comparedDevice) {
            ComparedDevice.DEVICE_A -> comparisonStatisticsA.add(incoming.rssi)
            ComparedDevice.DEVICE_B -> comparisonStatisticsB.add(incoming.rssi)
        }

        mutableUiState.update { state ->
            val comparison = state.compareDevicesState
            if (!comparison.isTracking) return@update state
            val currentSignal = when (comparedDevice) {
                ComparedDevice.DEVICE_A -> comparison.deviceA
                ComparedDevice.DEVICE_B -> comparison.deviceB
            }
            if (!CompareDevicesSession.matches(currentSignal, incoming.id, incoming.address)) {
                return@update state
            }

            val timestamp = incoming.lastSeen
            val smoothedRssi = RssiSmoother.next(
                previousSmoothedRssi = currentSignal.smoothedRssi,
                currentRssi = incoming.rssi,
            )
            val samples = RssiSampleWindow.retainRecent(
                samples = currentSignal.samples + RssiSample(timestamp, incoming.rssi),
                nowMillis = timestamp,
            )
            val smoothedSamples = RssiSampleWindow.retainRecentSmoothed(
                samples = currentSignal.smoothedSamples +
                    SmoothedRssiSample(timestamp, smoothedRssi),
                nowMillis = timestamp,
            )
            val updatedSignal = currentSignal.copy(
                device = currentSignal.device?.mergeLatest(incoming) ?: incoming,
                currentRssi = incoming.rssi,
                smoothedRssi = smoothedRssi,
                samples = samples,
                smoothedSamples = smoothedSamples,
                statistics = statistics,
                stability = SignalStabilityCalculator.calculate(
                    samples = samples,
                    smoothedSamples = smoothedSamples,
                    nowMillis = timestamp,
                ),
                lastSeen = timestamp,
                isSignalStale = false,
                isSignalLost = false,
            )
            val withUpdatedSignal = when (comparedDevice) {
                ComparedDevice.DEVICE_A -> comparison.copy(deviceA = updatedSignal)
                ComparedDevice.DEVICE_B -> comparison.copy(deviceB = updatedSignal)
            }
            val comparisonResult = if (
                withUpdatedSignal.deviceA.isSignalLost ||
                withUpdatedSignal.deviceB.isSignalLost
            ) {
                SignalComparisonResult(
                    differenceDb = null,
                    strongerSignal = StrongerSignal.UNAVAILABLE,
                )
            } else {
                SignalComparisonCalculator.calculate(
                    withUpdatedSignal.deviceA.smoothedRssi,
                    withUpdatedSignal.deviceB.smoothedRssi,
                )
            }
            val existingIndex = state.devices.indexOfFirst { it.id == incoming.id }
            val updatedDevices = if (existingIndex >= 0) {
                state.devices.toMutableList().apply {
                    set(existingIndex, state.devices[existingIndex].mergeLatest(incoming))
                }
            } else {
                state.devices + incoming
            }
            state.copy(
                devices = updatedDevices,
                selectedDevice = state.selectedDevice?.let { selected ->
                    if (selected.id == incoming.id) selected.mergeLatest(incoming) else selected
                },
                compareDevicesState = withUpdatedSignal.copy(
                    differenceDb = comparisonResult.differenceDb,
                    strongerSignal = comparisonResult.strongerSignal,
                    graphTimeMillis = timestamp,
                    unavailableReason = null,
                ),
            )
        }
    }

    private fun handleComparisonFailure(error: BleScanError) {
        releaseComparisonScanner()
        mutableUiState.update { state ->
            state.copy(
                compareDevicesState = state.compareDevicesState.copy(
                    isTracking = false,
                    unavailableReason = error.kind.toTrackingUnavailableReason(),
                ),
                bluetoothEnabled = if (error.kind == BleScanErrorKind.BluetoothDisabled) {
                    false
                } else {
                    state.bluetoothEnabled
                },
            )
        }
    }

    private fun startComparisonStatusTicker() {
        comparisonStatusJob?.cancel()
        comparisonStatusJob = viewModelScope.launch {
            while (true) {
                delay(SignalTrackerConfig.STATUS_TICK_MILLIS)
                val now = System.currentTimeMillis()
                mutableUiState.update { state ->
                    val comparison = state.compareDevicesState
                    if (!comparison.isTracking) return@update state
                    val samplesA = RssiSampleWindow.retainRecent(
                        comparison.deviceA.samples,
                        now,
                    )
                    val samplesB = RssiSampleWindow.retainRecent(
                        comparison.deviceB.samples,
                        now,
                    )
                    val smoothedA = RssiSampleWindow.retainRecentSmoothed(
                        comparison.deviceA.smoothedSamples,
                        now,
                    )
                    val smoothedB = RssiSampleWindow.retainRecentSmoothed(
                        comparison.deviceB.smoothedSamples,
                        now,
                    )
                    val withRecentSamples = comparison.copy(
                        deviceA = comparison.deviceA.copy(
                            samples = samplesA,
                            smoothedSamples = smoothedA,
                            stability = SignalStabilityCalculator.calculate(
                                samplesA,
                                smoothedA,
                                now,
                            ),
                        ),
                        deviceB = comparison.deviceB.copy(
                            samples = samplesB,
                            smoothedSamples = smoothedB,
                            stability = SignalStabilityCalculator.calculate(
                                samplesB,
                                smoothedB,
                                now,
                            ),
                        ),
                    )
                    state.copy(
                        compareDevicesState = CompareDevicesSession.refreshAvailability(
                            withRecentSamples,
                            now,
                        ),
                    )
                }
            }
        }
    }

    fun resumeComparison() {
        val state = mutableUiState.value
        val comparison = state.compareDevicesState
        if (state.destination != AppDestination.CompareDevices || comparison.isTracking) return
        if (comparison.deviceA.device == null || comparison.deviceB.device == null) return
        val unavailableReason = trackingUnavailableReason(state)
        if (unavailableReason != null) {
            mutableUiState.update { current ->
                current.copy(
                    compareDevicesState = current.compareDevicesState.copy(
                        unavailableReason = unavailableReason,
                    ),
                )
            }
            return
        }

        releaseComparisonScanner()
        val now = System.currentTimeMillis()
        mutableUiState.update { current ->
            current.copy(
                compareDevicesState = current.compareDevicesState.copy(
                    isTracking = true,
                    trackingStartedAt = now,
                    unavailableReason = null,
                    graphTimeMillis = now,
                    deviceA = current.compareDevicesState.deviceA.copy(
                        isSignalStale = false,
                        isSignalLost = false,
                    ),
                    deviceB = current.compareDevicesState.deviceB.copy(
                        isSignalStale = false,
                        isSignalLost = false,
                    ),
                ),
            )
        }
        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleComparisonDeviceFound,
            onFailure = ::handleComparisonFailure,
        )) {
            BleScanStartResult.Started -> startComparisonStatusTicker()
            is BleScanStartResult.Failed -> handleComparisonFailure(result.error)
        }
    }

    private fun stopComparisonForUnavailable(reason: TrackingUnavailableReason) {
        releaseComparisonScanner()
        mutableUiState.update { state ->
            state.copy(
                compareDevicesState = state.compareDevicesState.copy(
                    isTracking = false,
                    graphTimeMillis = System.currentTimeMillis(),
                    unavailableReason = reason,
                ),
            )
        }
    }

    private fun stopComparisonForNavigation() {
        releaseComparisonScanner()
        mutableUiState.update { state ->
            state.copy(
                compareDevicesState = state.compareDevicesState.copy(
                    isTracking = false,
                    unavailableReason = null,
                ),
            )
        }
    }

    private fun releaseComparisonScanner() {
        comparisonStatusJob?.cancel()
        comparisonStatusJob = null
        bleScanner.stopScan()
    }

    private fun trackingUnavailableReason(state: AppUiState): TrackingUnavailableReason? = when {
        !state.bleSupported -> TrackingUnavailableReason.BLE_UNSUPPORTED
        state.permissionState != BluetoothPermissionState.Granted ->
            TrackingUnavailableReason.PERMISSION_REQUIRED

        !state.bluetoothEnabled -> TrackingUnavailableReason.BLUETOOTH_DISABLED
        else -> null
    }

    fun onAppPaused() {
        stopScanning(markCompleted = false)
        if (mutableUiState.value.signalTrackerState.isTracking) {
            stopSignalTrackingForUnavailable(
                TrackingUnavailableReason.APP_BACKGROUNDED,
            )
        }
        if (mutableUiState.value.compareDevicesState.isTracking) {
            stopComparisonForUnavailable(TrackingUnavailableReason.APP_BACKGROUNDED)
        }
        if (mutableUiState.value.isAdvertisementInspectorRefreshing) {
            stopAdvertisementInspection()
        }
    }

    fun setFreezeEnabled(enabled: Boolean) {
        mutableUiState.update { state ->
            if (enabled) {
                state.copy(
                    freezeEnabled = true,
                    frozenDeviceIds = state.visibleDevices.map(BleDeviceInfo::id),
                ).refreshVisibleDevices()
            } else {
                state.copy(
                    freezeEnabled = false,
                    frozenDeviceIds = emptyList(),
                ).refreshVisibleDevices()
            }
        }
    }

    fun applyFilter(filterMode: DeviceFilterMode, minimumRssi: Int) {
        val normalizedMinimum = SettingsValueNormalizer.minimumRssi(minimumRssi)
        mutableUiState.update { state ->
            state.copy(
                filterMode = filterMode,
                minimumRssi = normalizedMinimum,
            ).refreshVisibleDevices()
        }
        viewModelScope.launch { settingsRepository.setMinimumRssi(normalizedMinimum) }
    }

    fun setSortMode(sortMode: DeviceSortMode) {
        mutableUiState.update { state ->
            state.copy(sortMode = sortMode).refreshVisibleDevices()
        }
    }

    fun openDevice(device: BleDeviceInfo) {
        stopScanning(markCompleted = false)
        mutableUiState.update {
            it.copy(
                destination = AppDestination.DeviceDetail,
                selectedDevice = device,
            )
        }
    }

    fun openTracker() {
        val device = mutableUiState.value.selectedDevice ?: return
        startSignalTracking(device)
    }

    fun openAdvertisementInspector() {
        val device = mutableUiState.value.selectedDevice ?: return
        stopScanning(markCompleted = false)
        releaseSignalTrackingScanner()
        releaseComparisonScanner()
        startAdvertisementInspection(device)
    }

    fun refreshAdvertisementInspector() {
        val state = mutableUiState.value
        if (state.destination != AppDestination.AdvertisementInspector) return
        state.selectedDevice?.let(::startAdvertisementInspection)
    }

    private fun startAdvertisementInspection(device: BleDeviceInfo) {
        releaseAdvertisementInspectorScanner()
        val currentState = mutableUiState.value
        val unavailableError = when {
            !currentState.bleSupported -> BleScanErrorKind.FeatureUnsupported
            currentState.permissionState != BluetoothPermissionState.Granted ->
                BleScanErrorKind.PermissionRequired

            !currentState.bluetoothEnabled -> BleScanErrorKind.BluetoothDisabled
            else -> null
        }
        mutableUiState.update { state ->
            state.copy(
                destination = AppDestination.AdvertisementInspector,
                selectedDevice = device,
                isAdvertisementInspectorRefreshing = unavailableError == null,
                advertisementInspectorTimeMillis = System.currentTimeMillis(),
                advertisementInspectorError = unavailableError,
            )
        }
        if (unavailableError != null) return

        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleAdvertisementInspectorDeviceFound,
            onFailure = ::handleAdvertisementInspectorFailure,
        )) {
            BleScanStartResult.Started -> startAdvertisementInspectorStatusTicker()
            is BleScanStartResult.Failed -> handleAdvertisementInspectorFailure(result.error)
        }
    }

    private fun handleAdvertisementInspectorDeviceFound(incoming: BleDeviceInfo) {
        mutableUiState.update { state ->
            val selected = state.selectedDevice ?: return@update state
            if (
                state.destination != AppDestination.AdvertisementInspector ||
                !state.isAdvertisementInspectorRefreshing ||
                !selected.matchesIdentity(incoming)
            ) {
                return@update state
            }
            val merged = selected.mergeLatest(incoming)
            val existingIndex = state.devices.indexOfFirst { it.id == merged.id }
            val updatedDevices = if (existingIndex >= 0) {
                state.devices.toMutableList().apply { set(existingIndex, merged) }
            } else {
                state.devices + merged
            }
            state.copy(
                devices = updatedDevices,
                selectedDevice = merged,
                advertisementInspectorTimeMillis = incoming.lastSeen,
                advertisementInspectorError = null,
            ).refreshVisibleDevices()
        }
    }

    private fun handleAdvertisementInspectorFailure(error: BleScanError) {
        stopAdvertisementInspection(error.kind)
    }

    private fun startAdvertisementInspectorStatusTicker() {
        advertisementInspectorStatusJob?.cancel()
        advertisementInspectorStatusJob = viewModelScope.launch {
            while (true) {
                delay(SignalTrackerConfig.STATUS_TICK_MILLIS)
                mutableUiState.update { state ->
                    if (
                        state.destination != AppDestination.AdvertisementInspector ||
                        !state.isAdvertisementInspectorRefreshing
                    ) {
                        return@update state
                    }
                    state.copy(advertisementInspectorTimeMillis = System.currentTimeMillis())
                }
            }
        }
    }

    private fun stopAdvertisementInspection(error: BleScanErrorKind? = null) {
        releaseAdvertisementInspectorScanner()
        mutableUiState.update { state ->
            state.copy(
                isAdvertisementInspectorRefreshing = false,
                advertisementInspectorTimeMillis = System.currentTimeMillis(),
                advertisementInspectorError = error,
            )
        }
    }

    private fun releaseAdvertisementInspectorScanner() {
        advertisementInspectorStatusJob?.cancel()
        advertisementInspectorStatusJob = null
        bleScanner.stopScan()
    }

    fun openBleEnvironment() {
        mutableUiState.update { it.copy(destination = AppDestination.BleEnvironment) }
    }

    fun resumeSignalTracking() {
        val state = mutableUiState.value
        val tracker = state.signalTrackerState
        if (state.destination != AppDestination.SignalTracker || tracker.isTracking) return

        val unavailableReason = when {
            !state.bleSupported -> TrackingUnavailableReason.BLE_UNSUPPORTED
            state.permissionState != BluetoothPermissionState.Granted ->
                TrackingUnavailableReason.PERMISSION_REQUIRED

            !state.bluetoothEnabled -> TrackingUnavailableReason.BLUETOOTH_DISABLED
            else -> null
        }
        if (unavailableReason != null) {
            mutableUiState.update { current ->
                current.copy(
                    signalTrackerState = current.signalTrackerState.copy(
                        unavailableReason = unavailableReason,
                    ),
                )
            }
            return
        }

        releaseSignalTrackingScanner()
        proximityAlertEvaluationState = if (tracker.proximityAlertEnabled) {
            ProximityAlertEvaluator.enabledInitialState(
                lastTriggeredAtMillis = proximityAlertEvaluationState.lastTriggeredAtMillis,
            )
        } else {
            proximityAlertEvaluationState.copy(
                status = ProximityAlertStatus.DISABLED,
                previousSmoothedRssi = null,
            )
        }
        mutableUiState.update { current ->
            current.copy(
                signalTrackerState = current.signalTrackerState.copy(
                    isTracking = true,
                    unavailableReason = null,
                    pendingVibrationEventId = null,
                    proximityAlertStatus = proximityAlertEvaluationState.status,
                    graphTimeMillis = System.currentTimeMillis(),
                ),
            )
        }

        when (val result = bleScanner.startScan(
            onDeviceFound = ::handleTrackingDeviceFound,
            onFailure = ::handleTrackingFailure,
        )) {
            BleScanStartResult.Started -> startTrackerStatusTicker()
            is BleScanStartResult.Failed -> handleTrackingFailure(result.error)
        }
    }

    fun setProximityAlertEnabled(enabled: Boolean) {
        proximityAlertEvaluationState = if (enabled) {
            ProximityAlertEvaluator.enabledInitialState(
                lastTriggeredAtMillis = proximityAlertEvaluationState.lastTriggeredAtMillis,
            )
        } else {
            proximityAlertEvaluationState.copy(
                status = ProximityAlertStatus.DISABLED,
                previousSmoothedRssi = null,
            )
        }
        mutableUiState.update { state ->
            state.copy(
                signalTrackerState = state.signalTrackerState.copy(
                    proximityAlertEnabled = enabled,
                    proximityAlertStatus = proximityAlertEvaluationState.status,
                    pendingVibrationEventId = null,
                ),
            )
        }
    }

    fun consumeProximityAlertVibration(eventId: Long) {
        mutableUiState.update { state ->
            if (state.signalTrackerState.pendingVibrationEventId != eventId) state
            else state.copy(
                signalTrackerState = state.signalTrackerState.copy(
                    pendingVibrationEventId = null,
                ),
            )
        }
    }

    fun openSettings() {
        stopScanning(markCompleted = false)
        mutableUiState.update {
            it.copy(
                destination = AppDestination.Settings,
                informationBackStack = emptyList(),
            )
        }
    }

    fun openPrivacyPolicy() = openInformation(AppDestination.PrivacyPolicy)

    fun openHowBleSignalsWork() = openInformation(AppDestination.HowBleSignalsWork)

    fun openAbout() = openInformation(AppDestination.About)

    private fun openInformation(destination: AppDestination) {
        mutableUiState.update { state -> state.openInformationDestination(destination) }
    }

    fun navigateBack() {
        val currentDestination = mutableUiState.value.destination
        if (currentDestination == AppDestination.SignalTracker) {
            stopSignalTrackingForNavigation()
        }
        if (currentDestination == AppDestination.CompareDevices) {
            stopComparisonForNavigation()
        }
        if (currentDestination == AppDestination.AdvertisementInspector) {
            stopAdvertisementInspection()
        }
        mutableUiState.update { state ->
            if (state.destination.isInformationDestination()) {
                return@update state.navigateBackFromInformation()
            }
            state.copy(
                destination = when (state.destination) {
                    AppDestination.SignalTracker -> AppDestination.DeviceDetail
                    AppDestination.CompareDevices,
                    AppDestination.CompareSelection,
                    AppDestination.AdvertisementInspector,
                    -> AppDestination.DeviceDetail

                    AppDestination.BleEnvironment,
                    AppDestination.DeviceDetail,
                    AppDestination.Settings,
                    AppDestination.Scanner,
                    -> AppDestination.Scanner

                    AppDestination.PrivacyPolicy,
                    AppDestination.HowBleSignalsWork,
                    AppDestination.About,
                    -> AppDestination.Settings
                },
            )
        }
    }

    fun backToScannerFromTracker() {
        stopSignalTrackingForNavigation()
        mutableUiState.update {
            it.copy(destination = AppDestination.Scanner).refreshVisibleDevices()
        }
    }

    fun backToScannerFromComparison() {
        stopComparisonForNavigation()
        mutableUiState.update {
            it.copy(destination = AppDestination.Scanner).refreshVisibleDevices()
        }
    }

    fun setScanDuration(seconds: Int) {
        if (seconds !in AppSettings.VALID_SCAN_DURATIONS) return
        mutableUiState.update { it.copy(scanDurationSeconds = seconds) }
        viewModelScope.launch { settingsRepository.setScanDuration(seconds) }
    }

    fun setShowUnnamedDevices(show: Boolean) {
        mutableUiState.update { state ->
            state.copy(showUnnamedDevices = show).refreshVisibleDevices()
        }
        viewModelScope.launch { settingsRepository.setShowUnnamedDevices(show) }
    }

    fun setMinimumRssi(rssi: Int) {
        val normalized = SettingsValueNormalizer.minimumRssi(rssi)
        mutableUiState.update { state ->
            state.copy(minimumRssi = normalized).refreshVisibleDevices()
        }
        viewModelScope.launch { settingsRepository.setMinimumRssi(normalized) }
    }

    fun setKeepScreenAwake(keepAwake: Boolean) {
        mutableUiState.update { it.copy(keepScreenAwake = keepAwake) }
        viewModelScope.launch { settingsRepository.setKeepScreenAwake(keepAwake) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        mutableUiState.update { it.copy(themeMode = themeMode) }
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun setSignalDescriptions(show: Boolean) {
        mutableUiState.update { it.copy(signalDescriptions = show) }
        viewModelScope.launch { settingsRepository.setSignalDescriptions(show) }
    }

    fun setProximityAlertThreshold(rssi: Int) {
        val normalized = SettingsValueNormalizer.proximityAlertThreshold(rssi)
        val alertEnabled = mutableUiState.value.signalTrackerState.proximityAlertEnabled
        proximityAlertEvaluationState = if (alertEnabled) {
            ProximityAlertEvaluator.enabledInitialState(
                lastTriggeredAtMillis = proximityAlertEvaluationState.lastTriggeredAtMillis,
            )
        } else {
            proximityAlertEvaluationState.copy(
                status = ProximityAlertStatus.DISABLED,
                previousSmoothedRssi = null,
            )
        }
        mutableUiState.update {
            it.copy(
                proximityAlertThreshold = normalized,
                signalTrackerState = it.signalTrackerState.copy(
                    proximityAlertStatus = proximityAlertEvaluationState.status,
                    pendingVibrationEventId = null,
                ),
            )
        }
        viewModelScope.launch {
            settingsRepository.setProximityAlertThreshold(normalized)
        }
    }

    override fun onCleared() {
        scanTimeoutJob?.cancel()
        trackerStatusJob?.cancel()
        comparisonStatusJob?.cancel()
        advertisementInspectorStatusJob?.cancel()
        bleScanner.stopScan()
    }
}

internal fun AppUiState.openInformationDestination(
    destination: AppDestination,
): AppUiState {
    require(destination.isInformationDestination()) {
        "Destination must be an informational page"
    }
    return copy(
        destination = destination,
        informationBackStack = informationBackStack + this.destination,
    )
}

internal fun AppUiState.navigateBackFromInformation(): AppUiState {
    if (!destination.isInformationDestination()) return this
    return copy(
        destination = informationBackStack.lastOrNull() ?: AppDestination.Settings,
        informationBackStack = informationBackStack.dropLast(1),
    )
}

private fun AppDestination.isInformationDestination(): Boolean = when (this) {
    AppDestination.PrivacyPolicy,
    AppDestination.HowBleSignalsWork,
    AppDestination.About,
    -> true

    else -> false
}

internal fun AppUiState.afterScanFailure(error: BleScanError): AppUiState = copy(
    isScanStarting = false,
    isScanning = false,
    hasCompletedScan = false,
    scanError = error.kind,
    bluetoothEnabled = if (error.kind == BleScanErrorKind.BluetoothDisabled) {
        false
    } else {
        bluetoothEnabled
    },
)

private fun AppUiState.refreshVisibleDevices(): AppUiState {
    if (!freezeEnabled) {
        return copy(
            visibleDevices = ScannerListProcessor.filterAndSort(
                devices = devices,
                filterMode = filterMode,
                minimumRssi = minimumRssi,
                showUnnamedDevices = showUnnamedDevices,
                sortMode = sortMode,
            ),
            frozenDeviceIds = emptyList(),
        )
    }

    val frozen = ScannerListProcessor.frozenVisibleDevices(
        devices = devices,
        frozenDeviceIds = frozenDeviceIds,
        filterMode = filterMode,
        minimumRssi = minimumRssi,
        showUnnamedDevices = showUnnamedDevices,
    )
    return copy(
        visibleDevices = frozen.devices,
        frozenDeviceIds = frozen.deviceIds,
    )
}

private fun BleDeviceInfo.mergeLatest(latest: BleDeviceInfo): BleDeviceInfo = latest.copy(
    name = latest.name ?: name,
    address = latest.address ?: address,
    manufacturerId = latest.manufacturerId ?: manufacturerId,
    manufacturerName = latest.manufacturerName ?: manufacturerName,
    manufacturerData = latest.manufacturerData ?: manufacturerData,
    manufacturerDataEntries = latest.manufacturerDataEntries.ifEmpty {
        manufacturerDataEntries
    },
    localName = latest.localName ?: localName,
    serviceUuids = latest.serviceUuids.ifEmpty { serviceUuids },
    serviceDataEntries = latest.serviceDataEntries.ifEmpty { serviceDataEntries },
    advertisementFlags = latest.advertisementFlags ?: advertisementFlags,
    rawAdvertisementBytes = latest.rawAdvertisementBytes ?: rawAdvertisementBytes,
    txPower = latest.txPower ?: txPower,
    isConnectable = latest.isConnectable ?: isConnectable,
)

private fun BleDeviceInfo.matchesIdentity(other: BleDeviceInfo): Boolean =
    id == other.id || (
        address != null && other.address != null && address.equals(other.address, ignoreCase = true)
    )
