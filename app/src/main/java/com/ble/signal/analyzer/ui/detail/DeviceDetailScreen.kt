package com.ble.signal.analyzer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.data.ble.BleManufacturerLookup
import com.ble.signal.analyzer.data.ble.BleServiceUuidFormatter
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.signalQualityFor
import com.ble.signal.analyzer.ui.components.InformationRow
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.components.SignalStrengthBars
import com.ble.signal.analyzer.ui.components.signalQualityColor
import com.ble.signal.analyzer.ui.manufacturerDisplayName
import com.ble.signal.analyzer.ui.signalQualityLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    device: BleDeviceInfo,
    showSignalDescription: Boolean,
    onBack: () -> Unit,
    onTrackSignal: () -> Unit,
    onCompare: () -> Unit,
    onOpenAdvertisementInspector: () -> Unit,
) {
    val quality = signalQualityFor(device.rssi)
    val qualityLabel = signalQualityLabel(quality)
    val strongerTrend = stringResource(R.string.getting_stronger)
    val measurementDescription = stringResource(
        R.string.signal_measurement_accessibility,
        device.rssi,
        qualityLabel,
        strongerTrend,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = device.name ?: stringResource(R.string.unknown_device),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_scanner),
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
            SectionLabel(text = stringResource(R.string.signal_section))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier
                        .clearAndSetSemantics {
                            contentDescription = measurementDescription
                        }
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = device.rssi.toString(),
                                style = MaterialTheme.typography.displayLarge,
                            )
                            Text(
                                text = stringResource(R.string.dbm_unit),
                                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        SignalStrengthBars(rssi = device.rssi, quality = quality)
                    }
                    if (showSignalDescription) {
                        Text(
                            text = qualityLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            color = signalQualityColor(quality),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.trend_stronger_format,
                            strongerTrend,
                        ),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Button(
                onClick = onTrackSignal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.track_signal))
            }
            OutlinedButton(
                onClick = onCompare,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.compare))
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.advanced_section))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenAdvertisementInspector,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.advertisement_inspector))
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.device_section))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InformationRow(
                        stringResource(R.string.name),
                        valueOrNotAvailable(device.name),
                    )
                    InformationRow(
                        stringResource(R.string.manufacturer),
                        manufacturerDisplayName(device.manufacturerId),
                    )
                    InformationRow(
                        stringResource(R.string.last_seen),
                        stringResource(R.string.now),
                    )
                    InformationRow(
                        label = stringResource(R.string.connectable),
                        value = when (device.isConnectable) {
                            true -> stringResource(R.string.yes)
                            false -> stringResource(R.string.no)
                            null -> stringResource(R.string.not_available)
                        },
                        showDivider = false,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.technical_information_section))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InformationRow(
                        label = stringResource(R.string.device_address),
                        value = valueOrNotAvailable(device.address),
                        supportingText = stringResource(R.string.randomized_address_note),
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
                        label = stringResource(R.string.tx_power),
                        value = device.txPower?.let {
                            stringResource(R.string.dbm_value, it)
                        } ?: stringResource(R.string.not_available),
                        showDivider = false,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun valueOrNotAvailable(value: String?): String =
    value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_available)
