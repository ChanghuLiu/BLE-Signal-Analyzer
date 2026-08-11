package com.ble.signal.analyzer.ui.advertisement

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.data.ble.AdvertisementDataFormatter
import com.ble.signal.analyzer.data.ble.BleManufacturerLookup
import com.ble.signal.analyzer.data.ble.BleScanErrorKind
import com.ble.signal.analyzer.data.ble.BleServiceUuidFormatter
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.ui.components.InformationRow
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.manufacturerDisplayName
import com.ble.signal.analyzer.ui.scanErrorMessage
import com.ble.signal.analyzer.ui.signalQualityLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertisementInspectorScreen(
    device: BleDeviceInfo,
    isRefreshing: Boolean,
    currentTimeMillis: Long,
    refreshError: BleScanErrorKind?,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val rawData = AdvertisementDataFormatter.formatHex(device.rawAdvertisementBytes)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.copied)
    val rawDataLabel = stringResource(R.string.raw_advertisement_data)
    var rawExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advertisement_inspector)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InspectorRefreshStatus(
                isRefreshing = isRefreshing,
                refreshError = refreshError,
                onRefresh = onRefresh,
            )

            SectionLabel(text = stringResource(R.string.device_section))
            InspectorInformationSurface {
                InformationRow(
                    stringResource(R.string.name),
                    device.name.orNotAvailable(),
                )
                InformationRow(
                    stringResource(R.string.device_address),
                    device.address.orNotAvailable(),
                )
                InformationRow(
                    stringResource(R.string.last_seen),
                    lastSeenText(currentTimeMillis, device.lastSeen),
                    showDivider = false,
                )
            }

            SectionLabel(text = stringResource(R.string.signal_section))
            InspectorInformationSurface {
                InformationRow(
                    stringResource(R.string.rssi),
                    stringResource(R.string.dbm_value, device.rssi),
                )
                InformationRow(
                    stringResource(R.string.signal_quality),
                    signalQualityLabel(signalQualityFor(device.rssi)),
                    showDivider = false,
                )
            }

            SectionLabel(text = stringResource(R.string.advertisement_section))
            InspectorInformationSurface {
                InformationRow(
                    stringResource(R.string.local_name),
                    device.localName.orNotAvailable(),
                )
                InformationRow(
                    stringResource(R.string.manufacturer),
                    manufacturerDisplayName(device.manufacturerId),
                )
                InformationRow(
                    stringResource(R.string.manufacturer_id),
                    BleManufacturerLookup.formatId(device.manufacturerId)
                        ?: stringResource(R.string.not_available),
                )
                InformationRow(
                    stringResource(R.string.service_uuids),
                    BleServiceUuidFormatter.formatListForDisplay(device.serviceUuids)
                        ?: stringResource(R.string.not_available),
                )
                InformationRow(
                    stringResource(R.string.tx_power),
                    device.txPower?.let { stringResource(R.string.dbm_value, it) }
                        ?: stringResource(R.string.not_available),
                )
                InformationRow(
                    stringResource(R.string.connectable),
                    when (device.isConnectable) {
                        true -> stringResource(R.string.yes)
                        false -> stringResource(R.string.no)
                        null -> stringResource(R.string.not_available)
                    },
                )
                InformationRow(
                    stringResource(R.string.advertisement_flags),
                    AdvertisementDataFormatter.formatFlags(device.advertisementFlags)
                        ?: stringResource(R.string.not_available),
                    showDivider = false,
                )
            }

            SectionLabel(text = stringResource(R.string.data_section))
            ManufacturerDataSection(device)
            ServiceDataSection(device)

            SectionLabel(text = stringResource(R.string.advanced_section))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rawDataLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { rawExpanded = !rawExpanded }) {
                            Text(
                                stringResource(
                                    if (rawExpanded) R.string.hide_raw_data
                                    else R.string.show_raw_data,
                                ),
                            )
                        }
                    }
                    if (rawExpanded) {
                        HexValue(rawData ?: stringResource(R.string.not_available))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText(rawDataLabel, rawData),
                                )
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(copiedMessage)
                                }
                            },
                            enabled = rawData != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Text(stringResource(R.string.copy_raw_advertisement_data))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InspectorRefreshStatus(
    isRefreshing: Boolean,
    refreshError: BleScanErrorKind?,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRefreshing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.refreshing_advertisement))
                }
            } else {
                refreshError?.let { error ->
                    Text(
                        text = scanErrorMessage(error),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (refreshError == null) 0.dp else 12.dp),
                ) {
                    Text(stringResource(R.string.refresh_advertisement))
                }
            }
        }
    }
}

@Composable
private fun ManufacturerDataSection(device: BleDeviceInfo) {
    val formatted = AdvertisementDataFormatter.formatManufacturerEntries(
        device.manufacturerDataEntries,
    )
    if (formatted.isEmpty()) {
        InspectorInformationSurface {
            InformationRow(
                stringResource(R.string.manufacturer_data),
                stringResource(R.string.not_available),
                showDivider = false,
            )
        }
        return
    }
    formatted.forEachIndexed { index, entry ->
        val manufacturerId = device.manufacturerDataEntries[index].manufacturerId
        InspectorInformationSurface {
            InformationRow(stringResource(R.string.manufacturer_id), entry.manufacturerId)
            InformationRow(
                stringResource(R.string.manufacturer),
                BleManufacturerLookup.nameFor(manufacturerId)
                    ?: stringResource(R.string.unknown_manufacturer),
            )
            HexInformationRow(
                label = stringResource(R.string.manufacturer_data),
                value = entry.data ?: stringResource(R.string.not_available),
            )
        }
    }
}

@Composable
private fun ServiceDataSection(device: BleDeviceInfo) {
    val formatted = AdvertisementDataFormatter.formatServiceDataEntries(device.serviceDataEntries)
    if (formatted.isEmpty()) {
        InspectorInformationSurface {
            InformationRow(
                stringResource(R.string.service_data),
                stringResource(R.string.not_available),
                showDivider = false,
            )
        }
        return
    }
    formatted.forEach { entry ->
        InspectorInformationSurface {
            InformationRow(stringResource(R.string.service_uuid), entry.serviceUuid)
            HexInformationRow(
                label = stringResource(R.string.service_data),
                value = entry.data ?: stringResource(R.string.not_available),
            )
        }
    }
}

@Composable
private fun InspectorInformationSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun HexInformationRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        HexValue(value)
    }
}

@Composable
private fun HexValue(value: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun String?.orNotAvailable(): String =
    this?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_available)

@Composable
private fun lastSeenText(currentTimeMillis: Long, lastSeenMillis: Long): String {
    val elapsedSeconds = ((currentTimeMillis - lastSeenMillis).coerceAtLeast(0L) / 1_000L).toInt()
    return when {
        elapsedSeconds <= 1 -> stringResource(R.string.now)
        elapsedSeconds < 60 -> stringResource(R.string.seconds_ago, elapsedSeconds)
        else -> stringResource(R.string.minutes_ago, elapsedSeconds / 60)
    }
}
