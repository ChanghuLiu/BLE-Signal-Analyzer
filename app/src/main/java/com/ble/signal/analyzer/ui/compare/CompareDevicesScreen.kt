package com.ble.signal.analyzer.ui.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.BluetoothPermissionState
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.signal.CompareDevicesSession
import com.ble.signal.analyzer.signal.CompareDevicesState
import com.ble.signal.analyzer.signal.ComparedDeviceSignalState
import com.ble.signal.analyzer.signal.RssiSample
import com.ble.signal.analyzer.signal.SignalTrackerConfig
import com.ble.signal.analyzer.signal.StrongerSignal
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.components.SignalStabilityCard
import com.ble.signal.analyzer.ui.components.SignalStrengthBars
import com.ble.signal.analyzer.ui.components.signalQualityColor
import com.ble.signal.analyzer.ui.manufacturerDisplayName
import com.ble.signal.analyzer.ui.signalQualityLabel
import com.ble.signal.analyzer.ui.trackingUnavailableMessage

private const val GRAPH_MIN_RSSI = -100f
private const val GRAPH_MAX_RSSI = -30f
private val GRAPH_HEIGHT = 200.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareDeviceSelectionScreen(
    deviceA: BleDeviceInfo,
    devices: List<BleDeviceInfo>,
    showSignalDescription: Boolean,
    showSameDeviceError: Boolean,
    onDeviceBSelected: (BleDeviceInfo) -> Unit,
    onBack: () -> Unit,
    onBackToScanner: () -> Unit,
) {
    val candidates = devices.filter {
        CompareDevicesSession.canSelectTogether(deviceA, it)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_second_device)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionLabel(text = stringResource(R.string.selected_device_a))
                Spacer(modifier = Modifier.height(8.dp))
                ComparisonSelectionDeviceCard(
                    device = deviceA,
                    deviceLabel = stringResource(R.string.device_a),
                    showSignalDescription = showSignalDescription,
                )
            }
            if (showSameDeviceError) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = stringResource(R.string.same_device_comparison_error),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            item { SectionLabel(text = stringResource(R.string.select_device_b)) }
            if (candidates.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.no_other_devices))
                            Button(
                                onClick = onBackToScanner,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .heightIn(min = 52.dp),
                            ) {
                                Text(stringResource(R.string.back_to_scanner_button))
                            }
                        }
                    }
                }
            } else {
                items(candidates, key = { it.id }) { device ->
                    val deviceName = device.name ?: stringResource(R.string.unknown_device)
                    ComparisonSelectionDeviceCard(
                        device = device,
                        deviceLabel = stringResource(R.string.device_b),
                        showSignalDescription = showSignalDescription,
                        onClick = { onDeviceBSelected(device) },
                        actionDescription = stringResource(
                            R.string.select_as_device_b,
                            deviceName,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonSelectionDeviceCard(
    device: BleDeviceInfo,
    deviceLabel: String,
    showSignalDescription: Boolean,
    onClick: (() -> Unit)? = null,
    actionDescription: String? = null,
) {
    val name = device.name ?: stringResource(R.string.unknown_device)
    val manufacturer = manufacturerDisplayName(device.manufacturerId)
    val quality = signalQualityFor(device.rssi)
    val qualityLabel = signalQualityLabel(quality)
    val accessibilityDescription = actionDescription ?: stringResource(
        R.string.compare_device_accessibility,
        name,
        manufacturer,
        device.rssi,
        qualityLabel,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics { contentDescription = accessibilityDescription }
                } else {
                    Modifier.semantics { contentDescription = accessibilityDescription }
                },
            ),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = deviceLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = manufacturer,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dbm_value, device.rssi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (showSignalDescription) {
                        Text(
                            text = qualityLabel,
                            color = signalQualityColor(quality),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareDevicesScreen(
    state: CompareDevicesState,
    keepScreenAwake: Boolean,
    bleSupported: Boolean,
    bluetoothEnabled: Boolean,
    permissionState: BluetoothPermissionState,
    onResumeComparison: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onExportComparison: () -> Unit,
    onBack: () -> Unit,
    onBackToScanner: () -> Unit,
) {
    val deviceA = state.deviceA.device ?: return
    val deviceB = state.deviceB.device ?: return
    val deviceARoleLabel = stringResource(R.string.device_a)
    val deviceBRoleLabel = stringResource(R.string.device_b)
    val localizedGenericNames = listOf(
        stringResource(R.string.unknown_device),
        deviceARoleLabel,
        deviceBRoleLabel,
    )
    val deviceALabel = formatComparisonDeviceLabel(
        roleLabel = deviceARoleLabel,
        deviceName = deviceA.name,
        localizedGenericNames = localizedGenericNames,
    )
    val deviceBLabel = formatComparisonDeviceLabel(
        roleLabel = deviceBRoleLabel,
        deviceName = deviceB.name,
        localizedGenericNames = localizedGenericNames,
    )
    KeepComparisonScreenAwake(keepScreenAwake && state.isTracking)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_devices)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComparisonAvailabilityCard(
                state = state,
                bleSupported = bleSupported,
                bluetoothEnabled = bluetoothEnabled,
                permissionState = permissionState,
                onResumeComparison = onResumeComparison,
                onRequestBluetoothPermission = onRequestBluetoothPermission,
                onOpenAppSettings = onOpenAppSettings,
                onEnableBluetooth = onEnableBluetooth,
                onBackToScanner = onBackToScanner,
            )

            ComparedDeviceSummary(
                label = deviceALabel,
                signal = state.deviceA,
            )
            ComparedDeviceSummary(
                label = deviceBLabel,
                signal = state.deviceB,
            )

            SectionLabel(text = stringResource(R.string.signal_difference))
            SignalDifferenceCard(
                state = state,
                deviceALabel = deviceALabel,
                deviceBLabel = deviceBLabel,
            )

            SectionLabel(text = stringResource(R.string.signal_stability_section))
            Text(
                text = deviceALabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SignalStabilityCard(result = state.deviceA.stability)
            Text(
                text = deviceBLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SignalStabilityCard(result = state.deviceB.stability)

            SectionLabel(text = stringResource(R.string.real_time_comparison))
            ComparisonGraph(
                samplesA = state.deviceA.samples,
                samplesB = state.deviceB.samples,
                graphTimeMillis = state.graphTimeMillis,
                deviceALabel = deviceALabel,
                deviceBLabel = deviceBLabel,
            )

            SectionLabel(text = stringResource(R.string.comparison_statistics))
            ComparisonStatisticsCard(
                label = deviceALabel,
                signal = state.deviceA,
            )
            ComparisonStatisticsCard(
                label = deviceBLabel,
                signal = state.deviceB,
            )
            val hasComparisonData = state.deviceA.samples.isNotEmpty() ||
                state.deviceB.samples.isNotEmpty()
            OutlinedButton(
                onClick = onExportComparison,
                enabled = hasComparisonData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.export_comparison))
            }
            if (!hasComparisonData) {
                Text(
                    text = stringResource(R.string.no_comparison_data_to_export),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ComparedDeviceSummary(
    label: String,
    signal: ComparedDeviceSignalState,
) {
    val device = signal.device ?: return
    val currentRssi = signal.currentRssi ?: device.rssi
    val quality = signalQualityFor(currentRssi)
    val signalStatus = when {
        signal.isSignalLost -> stringResource(R.string.device_signal_not_detected)
        signal.isSignalStale -> stringResource(R.string.waiting_for_device_signal)
        signal.samples.isEmpty() -> stringResource(R.string.collecting_signal)
        else -> null
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manufacturerDisplayName(device.manufacturerId),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dbm_value, currentRssi),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SignalStrengthBars(rssi = currentRssi, quality = quality)
                        Text(
                            text = signalQualityLabel(quality),
                            color = signalQualityColor(quality),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            signalStatus?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 8.dp),
                    color = if (signal.isSignalLost) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SignalDifferenceCard(
    state: CompareDevicesState,
    deviceALabel: String,
    deviceBLabel: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.differenceDb?.let {
                    stringResource(R.string.difference_db, it)
                } ?: stringResource(R.string.comparison_signal_unavailable),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            val explanation = when (state.strongerSignal) {
                StrongerSignal.DEVICE_A -> stringResource(
                    R.string.stronger_signal_value,
                    stringResource(R.string.stronger_signal),
                    deviceALabel,
                )

                StrongerSignal.DEVICE_B -> stringResource(
                    R.string.stronger_signal_value,
                    stringResource(R.string.stronger_signal),
                    deviceBLabel,
                )

                StrongerSignal.SIMILAR -> stringResource(R.string.signals_are_similar)
                StrongerSignal.UNAVAILABLE -> stringResource(R.string.signal_unavailable)
            }
            Text(
                text = explanation,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ComparisonAvailabilityCard(
    state: CompareDevicesState,
    bleSupported: Boolean,
    bluetoothEnabled: Boolean,
    permissionState: BluetoothPermissionState,
    onResumeComparison: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBackToScanner: () -> Unit,
) {
    if (state.isTracking) return
    val title: String
    val description: String
    val actionLabel: String
    val action: () -> Unit
    when {
        !bleSupported -> {
            title = stringResource(R.string.bluetooth_unavailable)
            description = stringResource(R.string.ble_not_supported)
            actionLabel = stringResource(R.string.back_to_scanner_button)
            action = onBackToScanner
        }

        permissionState != BluetoothPermissionState.Granted -> {
            title = stringResource(R.string.bluetooth_permission_required)
            description = stringResource(R.string.bluetooth_permission_permanent_description)
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
            actionLabel = stringResource(R.string.enable_bluetooth)
            action = onEnableBluetooth
        }

        else -> {
            title = stringResource(R.string.comparison_paused)
            description = state.unavailableReason?.let { trackingUnavailableMessage(it) }
                ?: stringResource(R.string.comparison_paused)
            actionLabel = stringResource(R.string.resume_comparison)
            action = onResumeComparison
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun ComparisonGraph(
    samplesA: List<RssiSample>,
    samplesB: List<RssiSample>,
    graphTimeMillis: Long,
    deviceALabel: String,
    deviceBLabel: String,
) {
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val graphDescription = stringResource(R.string.comparison_graph_accessibility_description)
    val referenceTime = maxOf(
        graphTimeMillis,
        samplesA.lastOrNull()?.timestamp ?: graphTimeMillis,
        samplesB.lastOrNull()?.timestamp ?: graphTimeMillis,
    )
    val windowStart = referenceTime - SignalTrackerConfig.GRAPH_WINDOW_MILLIS
    val yAxisLabels = listOf(-40, -60, -80, -100)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GraphLegendItem(
                    label = deviceALabel,
                    lineDescription = stringResource(R.string.solid_line),
                    color = colorA,
                    dashed = false,
                    modifier = Modifier.weight(1f),
                )
                GraphLegendItem(
                    label = deviceBLabel,
                    lineDescription = stringResource(R.string.dashed_line),
                    color = colorB,
                    dashed = true,
                    modifier = Modifier.weight(1f),
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
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
                                .semantics { contentDescription = graphDescription },
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
                            drawRssiLine(
                                samples = samplesA,
                                windowStart = windowStart,
                                referenceTime = referenceTime,
                                color = colorA,
                                dashed = false,
                            )
                            drawRssiLine(
                                samples = samplesB,
                                windowStart = windowStart,
                                referenceTime = referenceTime,
                                color = colorB,
                                dashed = true,
                            )
                        }
                        if (samplesA.isEmpty() && samplesB.isEmpty()) {
                            Text(
                                text = stringResource(R.string.waiting_for_rssi_samples),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(stringResource(R.string.graph_30_seconds_ago))
                    Text(stringResource(R.string.graph_15_seconds_ago))
                    Text(stringResource(R.string.now))
                }
            }
        }
    }
}

private fun DrawScope.drawRssiLine(
    samples: List<RssiSample>,
    windowStart: Long,
    referenceTime: Long,
    color: Color,
    dashed: Boolean,
) {
    val visible = samples.filter { it.timestamp in windowStart..referenceTime }
    if (visible.isEmpty()) return
    val path = Path()
    visible.forEachIndexed { index, sample ->
        val xFraction = ((sample.timestamp - windowStart).toFloat() /
            SignalTrackerConfig.GRAPH_WINDOW_MILLIS).coerceIn(0f, 1f)
        val yFraction = ((GRAPH_MAX_RSSI - sample.rssi) /
            (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)).coerceIn(0f, 1f)
        val point = Offset(xFraction * size.width, yFraction * size.height)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    if (visible.size > 1) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
                } else {
                    null
                },
            ),
        )
    }
    val last = visible.last()
    val lastX = ((last.timestamp - windowStart).toFloat() /
        SignalTrackerConfig.GRAPH_WINDOW_MILLIS).coerceIn(0f, 1f) * size.width
    val lastY = ((GRAPH_MAX_RSSI - last.rssi) /
        (GRAPH_MAX_RSSI - GRAPH_MIN_RSSI)).coerceIn(0f, 1f) * size.height
    if (dashed) {
        drawRect(
            color = color,
            topLeft = Offset(lastX - 4.dp.toPx(), lastY - 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()),
        )
    } else {
        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Composable
private fun GraphLegendItem(
    label: String,
    lineDescription: String,
    color: Color,
    dashed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.semantics {
            contentDescription = "$label, $lineDescription"
        },
    ) {
        Canvas(modifier = Modifier.size(width = 36.dp, height = 12.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3.dp.toPx(),
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
                } else {
                    null
                },
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ComparisonStatisticsCard(
    label: String,
    signal: ComparedDeviceSignalState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                ComparisonStat(
                    stringResource(R.string.current_stat),
                    signal.currentRssi,
                    Modifier.weight(1f),
                )
                ComparisonStat(
                    stringResource(R.string.average_stat),
                    signal.statistics?.average,
                    Modifier.weight(1f),
                )
                ComparisonStat(
                    stringResource(R.string.minimum_stat),
                    signal.statistics?.min,
                    Modifier.weight(1f),
                )
                ComparisonStat(
                    stringResource(R.string.maximum_stat),
                    signal.statistics?.max,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ComparisonStat(label: String, value: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KeepComparisonScreenAwake(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val wasKeepingScreenAwake = view.keepScreenOn
        if (enabled) view.keepScreenOn = true
        onDispose {
            if (enabled) view.keepScreenOn = wasKeepingScreenAwake
        }
    }
}
