package com.ble.signal.analyzer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.signal.SignalStabilityResult
import com.ble.signal.analyzer.ui.signalStabilityLabel

@Composable
fun SignalStabilityCard(
    result: SignalStabilityResult,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (result.score == null) {
                Text(
                    text = stringResource(R.string.stability_collecting),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = stringResource(R.string.stability_score_value, result.score),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = signalStabilityLabel(result.label),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (result.standardDeviation != null || result.rangeDb != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StabilityDetail(
                        label = stringResource(R.string.standard_deviation),
                        value = result.standardDeviation?.let {
                            stringResource(R.string.standard_deviation_value, it)
                        } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    StabilityDetail(
                        label = stringResource(R.string.signal_range),
                        value = result.rangeDb?.let {
                            stringResource(R.string.range_db_value, it)
                        } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StabilityDetail(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
