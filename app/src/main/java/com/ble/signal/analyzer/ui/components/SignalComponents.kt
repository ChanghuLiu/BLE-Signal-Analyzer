package com.ble.signal.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.model.SignalQuality
import com.ble.signal.analyzer.ui.theme.SignalExcellent
import com.ble.signal.analyzer.ui.theme.SignalFair
import com.ble.signal.analyzer.ui.theme.SignalGood
import com.ble.signal.analyzer.ui.theme.SignalStrong
import com.ble.signal.analyzer.ui.theme.SignalWeak

@Composable
fun signalQualityColor(quality: SignalQuality): Color = when (quality) {
    SignalQuality.Excellent -> SignalExcellent
    SignalQuality.Strong -> SignalStrong
    SignalQuality.Good -> SignalGood
    SignalQuality.Fair -> SignalFair
    SignalQuality.Weak -> SignalWeak
}

@Composable
fun SignalStrengthBars(
    rssi: Int,
    quality: SignalQuality,
    modifier: Modifier = Modifier,
) {
    val activeBars = when {
        rssi >= -59 -> 4
        rssi >= -69 -> 3
        rssi >= -79 -> 2
        else -> 1
    }
    val activeColor = signalQualityColor(quality)
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val heights = listOf(8.dp, 13.dp, 18.dp, 23.dp)

    Row(
        // Numeric RSSI and quality semantics are supplied by the containing measurement.
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height)
                    .background(
                        color = if (index < activeBars) activeColor else inactiveColor,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}
