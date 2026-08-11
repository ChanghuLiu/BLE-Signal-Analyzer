package com.ble.signal.analyzer.ui.environment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.environment.BleActivityLevel
import com.ble.signal.analyzer.environment.BleEnvironmentAnalyzer
import com.ble.signal.analyzer.model.BleDeviceInfo
import com.ble.signal.analyzer.model.SignalQuality
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.components.signalQualityColor
import com.ble.signal.analyzer.ui.signalQualityLabel
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleEnvironmentScreen(
    devices: List<BleDeviceInfo>,
    isScanning: Boolean,
    onScanAgain: () -> Unit,
    onExportEnvironment: () -> Unit,
    onBack: () -> Unit,
) {
    val summary = remember(devices) { BleEnvironmentAnalyzer.analyze(devices) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ble_environment)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isScanning) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(stringResource(R.string.scanning))
                    }
                }
            }

            if (summary.totalDevices == 0) {
                item {
                    EnvironmentEmptyState(
                        isScanning = isScanning,
                        onScanAgain = onScanAgain,
                    )
                }
            } else {
                item { SectionLabel(text = stringResource(R.string.ble_activity)) }
                item {
                    ActivityCard(activityLevel = summary.activityLevel)
                }
                item { SectionLabel(text = stringResource(R.string.environment_summary)) }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EnvironmentMetricRow(
                                stringResource(R.string.nearby_ble_devices),
                                summary.totalDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.named_devices),
                                summary.namedDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.unknown_devices),
                                summary.unknownDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.connectable_devices),
                                summary.connectableDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.strong_signals),
                                summary.strongDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.medium_signals),
                                summary.mediumDevices,
                            )
                            EnvironmentMetricRow(
                                stringResource(R.string.weak_signals),
                                summary.weakDevices,
                                showDivider = false,
                            )
                        }
                    }
                }

                item { SectionLabel(text = stringResource(R.string.signal_distribution)) }
                item {
                    val distribution = summary.signalDistribution
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            DistributionBar(
                                quality = SignalQuality.Excellent,
                                count = distribution.excellent,
                                total = summary.totalDevices,
                            )
                            DistributionBar(
                                quality = SignalQuality.Strong,
                                count = distribution.strong,
                                total = summary.totalDevices,
                            )
                            DistributionBar(
                                quality = SignalQuality.Good,
                                count = distribution.good,
                                total = summary.totalDevices,
                            )
                            DistributionBar(
                                quality = SignalQuality.Fair,
                                count = distribution.fair,
                                total = summary.totalDevices,
                            )
                            DistributionBar(
                                quality = SignalQuality.Weak,
                                count = distribution.weak,
                                total = summary.totalDevices,
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.environment_session_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = onExportEnvironment,
                    enabled = summary.totalDevices > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.export_environment))
                }
            }
            if (summary.totalDevices == 0) {
                item {
                    Text(
                        text = stringResource(R.string.no_environment_data_to_export),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.randomized_address_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ActivityCard(activityLevel: BleActivityLevel) {
    val label = stringResource(
        when (activityLevel) {
            BleActivityLevel.LOW -> R.string.activity_low
            BleActivityLevel.MODERATE -> R.string.activity_moderate
            BleActivityLevel.HIGH -> R.string.activity_high
        },
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.ble_activity_explanation),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EnvironmentMetricRow(label: String, count: Int, showDivider: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DistributionBar(quality: SignalQuality, count: Int, total: Int) {
    val label = signalQualityLabel(quality)
    val description = stringResource(R.string.distribution_bar_description, label, count)
    val fraction = if (total <= 0) 0f else count.toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(text = count.toString(), fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(
                        color = signalQualityColor(quality),
                        shape = MaterialTheme.shapes.small,
                    ),
            )
        }
    }
}

@Composable
private fun EnvironmentEmptyState(isScanning: Boolean, onScanAgain: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.no_ble_devices_detected),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.environment_empty_help),
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onScanAgain,
                enabled = !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.scan_again))
            }
        }
    }
}
