package com.ble.signal.analyzer.ui.scanner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.BluetoothPermissionState
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.data.ble.BleManufacturerLookup
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.scanner.DeviceFilterMode
import com.ble.signal.analyzer.scanner.DeviceSortMode
import com.ble.signal.analyzer.scanner.ScannerListProcessor
import com.ble.signal.analyzer.ui.components.SignalStrengthBars
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    devices: List<BleDeviceInfo>,
    totalDeviceCount: Int,
    isScanStarting: Boolean,
    isScanning: Boolean,
    hasCompletedScan: Boolean,
    scanError: String?,
    bleSupported: Boolean,
    bluetoothEnabled: Boolean,
    permissionState: BluetoothPermissionState,
    permissionPromptDismissed: Boolean,
    filterMode: DeviceFilterMode,
    sortMode: DeviceSortMode,
    minimumRssi: Int,
    activeFilterCount: Int,
    freezeEnabled: Boolean,
    signalDescriptions: Boolean,
    onToggleScanning: () -> Unit,
    onFilterApplied: (DeviceFilterMode, Int) -> Unit,
    onSortChanged: (DeviceSortMode) -> Unit,
    onFreezeChanged: (Boolean) -> Unit,
    onDeviceSelected: (BleDeviceInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onPermissionNotNow: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var draftFilterMode by rememberSaveable { mutableStateOf(filterMode) }
    var draftSortMode by rememberSaveable { mutableStateOf(sortMode) }
    var draftMinimumRssi by rememberSaveable { mutableFloatStateOf(minimumRssi.toFloat()) }
    val freezeDescription = stringResource(R.string.freeze_device_order)
    val freezeState = stringResource(
        if (freezeEnabled) R.string.enabled_state else R.string.disabled_state,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.open_settings),
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
            when {
                !bleSupported -> item {
                    UnsupportedBleState()
                }

                permissionState == BluetoothPermissionState.NotRequested -> item {
                    PermissionExplanationState(
                        onAllowBluetoothScan = onRequestBluetoothPermission,
                    )
                }

                permissionState == BluetoothPermissionState.PermanentlyDenied -> item {
                    PermissionPermanentlyDeniedState(onOpenSettings = onOpenAppSettings)
                }

                permissionState == BluetoothPermissionState.Denied &&
                    !permissionPromptDismissed -> item {
                    PermissionDeniedState(
                        onGrantPermission = onRequestBluetoothPermission,
                        onNotNow = onPermissionNotNow,
                    )
                }

                permissionState == BluetoothPermissionState.Denied -> item {
                    PermissionDeferredState(
                        onGrantPermission = onRequestBluetoothPermission,
                    )
                }

                !bluetoothEnabled -> item {
                    BluetoothDisabledState(onEnableBluetooth = onEnableBluetooth)
                }

                hasCompletedScan && totalDeviceCount == 0 && scanError == null -> item {
                    EmptyScanState(onScanAgain = onToggleScanning)
                }

                else -> {
                    item {
                        ScanStatusCard(
                            isScanning = isScanning,
                            hasCompletedScan = hasCompletedScan,
                        )
                    }
                    scanError?.let { message ->
                        item { ScanErrorCard(message = message) }
                    }
                    item {
                        Button(
                            onClick = onToggleScanning,
                            enabled = !isScanStarting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                        ) {
                            Text(
                                when {
                                    isScanStarting -> stringResource(R.string.starting)
                                    isScanning -> stringResource(R.string.stop)
                                    scanError != null -> stringResource(R.string.try_again)
                                    else -> stringResource(R.string.scan)
                                },
                            )
                        }
                    }
                    item {
                        Text(
                            text = if (devices.size == totalDeviceCount) {
                                pluralStringResource(
                                    R.plurals.ble_devices_nearby,
                                    totalDeviceCount,
                                    totalDeviceCount,
                                )
                            } else {
                                stringResource(
                                    R.string.ble_devices_shown,
                                    devices.size,
                                    totalDeviceCount,
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    draftFilterMode = filterMode
                                    draftMinimumRssi = minimumRssi.toFloat()
                                    showFilterDialog = true
                                },
                            ) {
                                Text(
                                    if (activeFilterCount == 0) {
                                        stringResource(R.string.filter)
                                    } else {
                                        stringResource(
                                            R.string.filter_active_count,
                                            activeFilterCount,
                                        )
                                    },
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    draftSortMode = sortMode
                                    showSortDialog = true
                                },
                            ) {
                                Text(stringResource(R.string.sort_active, sortMode.shortName))
                            }
                            FilterChip(
                                selected = freezeEnabled,
                                onClick = { onFreezeChanged(!freezeEnabled) },
                                label = { Text(stringResource(R.string.freeze)) },
                                modifier = Modifier.semantics {
                                    contentDescription = freezeDescription
                                    stateDescription = freezeState
                                },
                            )
                        }
                    }
                    if (devices.isEmpty() && totalDeviceCount > 0) {
                        item {
                            Text(
                                text = stringResource(R.string.no_filter_matches),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                    items(items = devices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            showSignalDescription = signalDescriptions,
                            onClick = { onDeviceSelected(device) },
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showFilterDialog && permissionState == BluetoothPermissionState.Granted) {
        FilterDialog(
            selected = draftFilterMode,
            minimumRssi = draftMinimumRssi,
            onSelected = { draftFilterMode = it },
            onMinimumRssiChanged = { draftMinimumRssi = it },
            onReset = {
                draftFilterMode = DeviceFilterMode.All
                draftMinimumRssi = ScannerListProcessor.DEFAULT_MINIMUM_RSSI.toFloat()
            },
            onApply = {
                onFilterApplied(draftFilterMode, draftMinimumRssi.roundToInt())
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }

    if (showSortDialog && permissionState == BluetoothPermissionState.Granted) {
        SortDialog(
            selected = draftSortMode,
            onSelected = { draftSortMode = it },
            onApply = {
                onSortChanged(draftSortMode)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false },
        )
    }
}

@Composable
private fun PermissionExplanationState(onAllowBluetoothScan: () -> Unit) {
    StateMessageCard(
        title = stringResource(R.string.permission_scan_title),
        description = stringResource(R.string.permission_scan_explanation),
    ) {
        Button(
            onClick = onAllowBluetoothScan,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.allow_bluetooth_scan))
        }
    }
}

@Composable
private fun PermissionDeniedState(
    onGrantPermission: () -> Unit,
    onNotNow: () -> Unit,
) {
    StateMessageCard(
        title = stringResource(R.string.bluetooth_permission_required),
        description = stringResource(R.string.bluetooth_permission_description),
        useErrorColors = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onGrantPermission,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.grant_permission))
            }
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.not_now))
            }
        }
    }
}

@Composable
private fun PermissionPermanentlyDeniedState(onOpenSettings: () -> Unit) {
    StateMessageCard(
        title = stringResource(R.string.bluetooth_permission_required),
        description = stringResource(R.string.bluetooth_permission_permanent_description),
        useErrorColors = true,
    ) {
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.open_android_settings))
        }
    }
}

@Composable
private fun PermissionDeferredState(onGrantPermission: () -> Unit) {
    StateMessageCard(
        title = stringResource(R.string.permission_required),
        description = stringResource(R.string.permission_deferred_description),
    ) {
        OutlinedButton(
            onClick = onGrantPermission,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

@Composable
private fun UnsupportedBleState() {
    StateMessageCard(
        title = stringResource(R.string.bluetooth_unavailable),
        description = stringResource(R.string.ble_not_supported),
        useErrorColors = true,
    )
}

@Composable
private fun BluetoothDisabledState(onEnableBluetooth: () -> Unit) {
    StateMessageCard(
        title = stringResource(R.string.bluetooth_turned_off),
        description = stringResource(R.string.bluetooth_scan_requirement),
    ) {
        Button(
            onClick = onEnableBluetooth,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.enable_bluetooth))
        }
    }
}

@Composable
private fun EmptyScanState(onScanAgain: () -> Unit) {
    StateMessageCard(
        title = stringResource(R.string.no_ble_devices_found),
        description = stringResource(R.string.no_ble_devices_help),
    ) {
        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.scan_again))
        }
    }
}

@Composable
private fun ScanErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.scan_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun StateMessageCard(
    title: String,
    description: String,
    useErrorColors: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val containerColor = if (useErrorColors) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (useErrorColors) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
            action?.let {
                Spacer(modifier = Modifier.height(20.dp))
                it()
            }
        }
    }
}

@Composable
private fun ScanStatusCard(isScanning: Boolean, hasCompletedScan: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    ) {}
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when {
                        isScanning -> stringResource(R.string.scanning)
                        hasCompletedScan -> stringResource(R.string.scan_complete)
                        else -> stringResource(R.string.ready_to_scan)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isScanning) {
                        stringResource(R.string.listening_for_ble)
                    } else {
                        stringResource(R.string.results_local)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: BleDeviceInfo,
    showSignalDescription: Boolean,
    onClick: () -> Unit,
) {
    val quality = signalQualityFor(device.rssi)
    val deviceName = device.name?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.unknown_device)
    val manufacturerName = BleManufacturerLookup.displayNameFor(device.manufacturerId)
    val lastSeen = lastSeenLabel(device.lastSeen)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$deviceName, $manufacturerName, Signal " +
                    "${device.rssi} dBm, ${quality.label}, Last seen $lastSeen"
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = manufacturerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SignalStrengthBars(rssi = device.rssi, quality = quality)
                    if (showSignalDescription) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = lastSeen,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FilterDialog(
    selected: DeviceFilterMode,
    minimumRssi: Float,
    onSelected: (DeviceFilterMode) -> Unit,
    onMinimumRssiChanged: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val minimumRssiDescription = stringResource(R.string.minimum_rssi)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_devices)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.device_type),
                    style = MaterialTheme.typography.titleMedium,
                )
                DeviceFilterMode.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == option,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = null,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Text(option.displayName)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.minimum_rssi),
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = minimumRssi,
                    onValueChange = onMinimumRssiChanged,
                    modifier = Modifier.semantics {
                        contentDescription = minimumRssiDescription
                    },
                    valueRange = -100f..-30f,
                    steps = 69,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("-100 dBm", style = MaterialTheme.typography.bodyMedium)
                    Text("-30 dBm", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = stringResource(
                        R.string.current_dbm,
                        minimumRssi.roundToInt(),
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
        },
    )
}

@Composable
private fun SortDialog(
    selected: DeviceSortMode,
    onSelected: (DeviceSortMode) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_by)) },
        text = {
            Column {
                DeviceSortMode.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == option,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = null,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Text(option.displayName)
                    }
                }
                Text(
                    text = stringResource(R.string.strongest_first_help),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun lastSeenLabel(lastSeen: Long): String {
    val seconds = ((System.currentTimeMillis() - lastSeen).coerceAtLeast(0L) / 1_000L)
    return when {
        seconds < 2 -> stringResource(R.string.now)
        seconds < 60 -> stringResource(R.string.seconds_ago, seconds)
        else -> stringResource(R.string.minutes_ago, seconds / 60)
    }
}
