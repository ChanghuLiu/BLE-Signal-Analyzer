package com.ble.signal.analyzer.ui.tracker

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ble.signal.analyzer.BluetoothPermissionState
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.ProximityAlertStatus
import com.ble.signal.analyzer.signal.SignalTrackerConfig
import com.ble.signal.analyzer.signal.SignalTrackerState
import com.ble.signal.analyzer.signal.SignalTrend
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.components.SignalStabilityCard
import com.ble.signal.analyzer.ui.components.signalQualityColor
import com.ble.signal.analyzer.ui.proximityLabel
import com.ble.signal.analyzer.ui.signalQualityLabel
import com.ble.signal.analyzer.ui.trackingUnavailableMessage
import kotlin.math.roundToInt

private const val GRAPH_MIN_RSSI = -100f
private const val GRAPH_MAX_RSSI = -30f
private val GRAPH_HEIGHT = 200.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalTrackerScreen(
    device: BleDeviceInfo,
    trackerState: SignalTrackerState,
    showSignalDescription: Boolean,
    keepScreenAwake: Boolean,
    proximityAlertThreshold: Int,
    onProximityAlertThresholdChanged: (Int) -> Unit,
    bleSupported: Boolean,
    bluetoothEnabled: Boolean,
    permissionState: BluetoothPermissionState,
    onProximityAlertEnabledChanged: (Boolean) -> Unit,
    onVibrationConsumed: (Long) -> Unit,
    onResumeTracking: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBack: () -> Unit,
    onBackToScanner: () -> Unit,
) {
    val currentRssi = trackerState.currentRssi ?: device.rssi
    val quality = signalQualityFor(currentRssi)
    val qualityLabel = signalQualityLabel(quality)
    val currentSignalDescription = stringResource(
        R.string.current_signal_accessibility,
        currentRssi,
        qualityLabel,
    )
    var alertThreshold by rememberSaveable {
        mutableFloatStateOf(proximityAlertThreshold.toFloat())
    }
    val alertThresholdDescription = stringResource(R.string.proximity_alert_threshold)

    KeepScreenAwake(enabled = keepScreenAwake && trackerState.isTracking)
    ProximityVibrationEffect(
        eventId = trackerState.pendingVibrationEventId,
        isTracking = trackerState.isTracking,
        onConsumed = onVibrationConsumed,
    )
    LaunchedEffect(proximityAlertThreshold) {
        alertThreshold = proximityAlertThreshold.toFloat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signal_tracker)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_device_details),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = trackerState.deviceName.ifBlank {
                    device.name ?: stringResource(R.string.unknown_device)
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            TrackerAvailabilityCard(
                state = trackerState,
                bleSupported = bleSupported,
                bluetoothEnabled = bluetoothEnabled,
                permissionState = permissionState,
                onResumeTracking = onResumeTracking,
                onRequestBluetoothPermission = onRequestBluetoothPermission,
                onOpenAppSettings = onOpenAppSettings,
                onEnableBluetooth = onEnableBluetooth,
                onBackToScanner = onBackToScanner,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = currentSignalDescription
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = currentRssi.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.dbm_unit),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = trendText(trackerState.trend),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (showSignalDescription) {
                Text(
                    text = qualityLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clearAndSetSemantics { },
                    style = MaterialTheme.typography.titleLarge,
                    color = signalQualityColor(quality),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.signal_stability_section))
            Spacer(modifier = Modifier.height(8.dp))
            SignalStabilityCard(result = trackerState.stability)

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(text = stringResource(R.string.relative_proximity))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = trackerState.proximityLabel?.let { proximityLabel(it) }
                            ?: stringResource(R.string.collecting_signal),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.relative_proximity_note),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.last_30_seconds))
            Spacer(modifier = Modifier.height(8.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SignalGraph(
                    samples = trackerState.samples,
                    graphTimeMillis = trackerState.graphTimeMillis,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SignalStats(
                minimum = trackerState.minRssi,
                average = trackerState.averageRssi,
                maximum = trackerState.maxRssi,
            )

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.proximity_alert_section))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingSwitchRow(
                        title = stringResource(R.string.enable_vibration),
                        description = stringResource(R.string.vibration_explanation),
                        checked = trackerState.proximityAlertEnabled,
                        onCheckedChange = onProximityAlertEnabledChanged,
                    )
                    Text(
                        text = stringResource(
                            R.string.stronger_than_dbm,
                            alertThreshold.roundToInt(),
                        ),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        onValueChangeFinished = {
                            onProximityAlertThresholdChanged(alertThreshold.roundToInt())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = alertThresholdDescription
                            },
                        enabled = trackerState.proximityAlertEnabled,
                        valueRange = -90f..-35f,
                        steps = 54,
                    )
                    Text(
                        text = proximityAlertStatusText(
                            enabled = trackerState.proximityAlertEnabled,
                            status = trackerState.proximityAlertStatus,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeepScreenAwake(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val wasKeepingScreenAwake = view.keepScreenOn
        if (enabled) view.keepScreenOn = true

        onDispose {
            if (enabled) view.keepScreenOn = wasKeepingScreenAwake
        }
    }
}

@Composable
private fun ProximityVibrationEffect(
    eventId: Long?,
    isTracking: Boolean,
    onConsumed: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(eventId, isTracking) {
        val id = eventId ?: return@LaunchedEffect
        val isVisible = lifecycleOwner.lifecycle.currentState.isAtLeast(
            Lifecycle.State.RESUMED,
        )
        if (isTracking && isVisible) {
            vibrateProximityAlert(context)
        }
        onConsumed(id)
    }
}

private fun vibrateProximityAlert(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator?.hasVibrator() != true) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(
            SignalTrackerConfig.ALERT_VIBRATION_DURATION_MILLIS,
            VibrationEffect.DEFAULT_AMPLITUDE,
        ),
    )
}

@Composable
private fun proximityAlertStatusText(
    enabled: Boolean,
    status: ProximityAlertStatus,
): String = when {
    !enabled -> stringResource(R.string.vibration_alert_off)
    status == ProximityAlertStatus.OBSERVING ->
        stringResource(R.string.checking_current_signal)

    status == ProximityAlertStatus.READY -> stringResource(R.string.alert_ready)
    status == ProximityAlertStatus.WAITING_FOR_REARM ->
        stringResource(R.string.move_away_to_rearm)

    else -> stringResource(R.string.checking_current_signal)
}

@Composable
private fun TrackerAvailabilityCard(
    state: SignalTrackerState,
    bleSupported: Boolean,
    bluetoothEnabled: Boolean,
    permissionState: BluetoothPermissionState,
    onResumeTracking: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBackToScanner: () -> Unit,
) {
    val title: String
    val description: String
    val useErrorColors: Boolean
    val actionLabel: String?
    val action: (() -> Unit)?

    when {
        !bleSupported -> {
            title = stringResource(R.string.bluetooth_unavailable)
            description = stringResource(R.string.ble_not_supported)
            useErrorColors = true
            actionLabel = stringResource(R.string.back_to_scanner_button)
            action = onBackToScanner
        }

        permissionState != BluetoothPermissionState.Granted -> {
            title = stringResource(R.string.bluetooth_permission_required)
            description = stringResource(R.string.bluetooth_permission_permanent_description)
            useErrorColors = true
            if (permissionState == BluetoothPermissionState.PermanentlyDenied) {
                actionLabel = stringResource(R.string.open_android_settings)
                action = onOpenAppSettings
            } else {
                actionLabel = stringResource(R.string.grant_permission)
                action = onRequestBluetoothPermission
            }
        }

        !bluetoothEnabled -> {
            title = stringResource(R.string.bluetooth_turned_off)
            description = stringResource(R.string.bluetooth_tracker_requirement)
            useErrorColors = false
            actionLabel = stringResource(R.string.enable_bluetooth)
            action = onEnableBluetooth
        }

        state.unavailableReason != null -> {
            title = stringResource(R.string.tracking_paused)
            description = trackingUnavailableMessage(state.unavailableReason)
            useErrorColors = false
            actionLabel = stringResource(R.string.resume_tracking)
            action = onResumeTracking
        }

        state.isSignalLost -> {
            title = stringResource(R.string.device_signal_not_detected)
            description = stringResource(R.string.device_signal_not_detected_description)
            useErrorColors = true
            actionLabel = stringResource(R.string.back_to_scanner_button)
            action = onBackToScanner
        }

        state.isSignalStale -> {
            title = stringResource(R.string.signal_temporarily_unavailable)
            description = stringResource(R.string.waiting_for_device_signal)
            useErrorColors = false
            actionLabel = null
            action = null
        }

        state.samples.isEmpty() -> {
            title = stringResource(R.string.collecting_signal)
            description = stringResource(R.string.waiting_for_next_advertisement)
            useErrorColors = false
            actionLabel = null
            action = null
        }

        else -> return
    }

    val containerColor = if (useErrorColors) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (useErrorColors) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            if (actionLabel != null && action != null) {
                Button(
                    onClick = action,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun SignalGraph(
    samples: List<RssiSample>,
    graphTimeMillis: Long,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val graphDescription = stringResource(R.string.graph_accessibility_description)
    val referenceTime = maxOf(
        graphTimeMillis,
        samples.lastOrNull()?.timestamp ?: graphTimeMillis,
    )
    val windowStart = referenceTime - SignalTrackerConfig.GRAPH_WINDOW_MILLIS
    val yAxisLabels = listOf(-40, -60, -80, -100)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(GRAPH_HEIGHT),
                ) {
                    yAxisLabels.forEach { label ->
                        val fraction = ((GRAPH_MAX_RSSI - label) /
                            (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)).coerceIn(0f, 1f)
                        val offset = (GRAPH_HEIGHT * fraction - 8.dp)
                            .coerceIn(0.dp, GRAPH_HEIGHT - 16.dp)
                        Text(
                            text = label.toString(),
                            modifier = Modifier.offset(y = offset),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(GRAPH_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = graphDescription
                            },
                    ) {
                        yAxisLabels.forEach { label ->
                            val y = ((GRAPH_MAX_RSSI - label) /
                                (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)) * size.height
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        val visibleSamples = samples.filter {
                            it.timestamp >= windowStart && it.timestamp <= referenceTime
                        }
                        if (visibleSamples.isNotEmpty()) {
                            val path = Path()
                            visibleSamples.forEachIndexed { index, sample ->
                                val xFraction = ((sample.timestamp - windowStart).toFloat() /
                                    SignalTrackerConfig.GRAPH_WINDOW_MILLIS).coerceIn(0f, 1f)
                                val yFraction = ((GRAPH_MAX_RSSI - sample.rssi) /
                                    (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)).coerceIn(0f, 1f)
                                val x = xFraction * size.width
                                val y = yFraction * size.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (visibleSamples.size > 1) {
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    ),
                                )
                            }
                            val last = visibleSamples.last()
                            val lastX = (((last.timestamp - windowStart).toFloat() /
                                SignalTrackerConfig.GRAPH_WINDOW_MILLIS).coerceIn(0f, 1f)) *
                                size.width
                            val lastY = (((GRAPH_MAX_RSSI - last.rssi) /
                                (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)).coerceIn(0f, 1f)) *
                                size.height
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = Offset(lastX, lastY),
                            )
                        }
                    }
                    if (samples.isEmpty()) {
                        Text(
                            text = stringResource(R.string.waiting_for_rssi_samples),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.graph_30_seconds_ago),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.graph_15_seconds_ago),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.now), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SignalStats(minimum: Int?, average: Int?, maximum: Int?) {
    val useStackedLayout = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (useStackedLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatItem(
                    label = stringResource(R.string.minimum_stat),
                    value = minimum,
                    modifier = Modifier.fillMaxWidth(),
                )
                StatItem(
                    label = stringResource(R.string.average_stat),
                    value = average,
                    modifier = Modifier.fillMaxWidth(),
                )
                StatItem(
                    label = stringResource(R.string.maximum_stat),
                    value = maximum,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
            ) {
                StatItem(
                    label = stringResource(R.string.minimum_stat),
                    value = minimum,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.average_stat),
                    value = average,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.maximum_stat),
                    value = maximum,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.let { stringResource(R.string.dbm_value, it) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun trendText(trend: SignalTrend): String = when (trend) {
    SignalTrend.STRONGER -> stringResource(
        R.string.trend_stronger_format,
        stringResource(R.string.getting_stronger),
    )
    SignalTrend.WEAKER -> stringResource(
        R.string.trend_weaker_format,
        stringResource(R.string.getting_weaker),
    )
    SignalTrend.STABLE -> stringResource(
        R.string.trend_stable_format,
        stringResource(R.string.stable),
    )
    SignalTrend.COLLECTING -> stringResource(
        R.string.trend_collecting_format,
        stringResource(R.string.collecting_signal),
    )
}
