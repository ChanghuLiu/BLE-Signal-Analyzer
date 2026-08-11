package com.ble.signal.analyzer.signal

import kotlin.math.roundToInt

object RssiSmoother {
    fun next(
        previousSmoothedRssi: Double?,
        currentRssi: Int,
        alpha: Double = SignalTrackerConfig.EMA_ALPHA,
    ): Double {
        require(alpha in 0.0..1.0) { "EMA alpha must be between 0 and 1" }
        return previousSmoothedRssi?.let { previous ->
            alpha * currentRssi + (1.0 - alpha) * previous
        } ?: currentRssi.toDouble()
    }
}

object SignalTrendCalculator {
    fun calculate(
        samples: List<SmoothedRssiSample>,
        nowMillis: Long = samples.lastOrNull()?.timestamp ?: 0L,
        windowMillis: Long = SignalTrackerConfig.TREND_WINDOW_MILLIS,
        thresholdDbm: Double = SignalTrackerConfig.TREND_THRESHOLD_DBM,
    ): SignalTrend {
        if (samples.isEmpty()) return SignalTrend.COLLECTING

        val recentWindowStart = nowMillis - windowMillis
        val previousWindowStart = recentWindowStart - windowMillis
        val recentValues = samples
            .asSequence()
            .filter { it.timestamp > recentWindowStart && it.timestamp <= nowMillis }
            .map { it.rssi }
            .toList()
        val previousValues = samples
            .asSequence()
            .filter {
                it.timestamp > previousWindowStart && it.timestamp <= recentWindowStart
            }
            .map { it.rssi }
            .toList()

        if (recentValues.size < 2 || previousValues.size < 2) {
            return SignalTrend.COLLECTING
        }

        val change = recentValues.average() - previousValues.average()
        return when {
            change >= thresholdDbm -> SignalTrend.STRONGER
            change <= -thresholdDbm -> SignalTrend.WEAKER
            else -> SignalTrend.STABLE
        }
    }
}

data class RssiStatistics(
    val min: Int,
    val average: Int,
    val max: Int,
)

class SignalStatisticsAccumulator {
    private var minimum: Int? = null
    private var maximum: Int? = null
    private var sum = 0L
    private var count = 0L

    fun add(rssi: Int): RssiStatistics {
        minimum = minimum?.let { minOf(it, rssi) } ?: rssi
        maximum = maximum?.let { maxOf(it, rssi) } ?: rssi
        sum += rssi
        count += 1
        return snapshot()!!
    }

    fun snapshot(): RssiStatistics? {
        if (count == 0L) return null
        return RssiStatistics(
            min = minimum!!,
            average = (sum.toDouble() / count).roundToInt(),
            max = maximum!!,
        )
    }

    fun reset() {
        minimum = null
        maximum = null
        sum = 0L
        count = 0L
    }
}

object ProximityLabelMapper {
    fun fromSmoothedRssi(smoothedRssi: Double): ProximityLabel = when {
        smoothedRssi >= -45.0 -> ProximityLabel.VERY_CLOSE
        smoothedRssi >= -59.0 -> ProximityLabel.CLOSE
        smoothedRssi >= -69.0 -> ProximityLabel.NEARBY
        smoothedRssi >= -79.0 -> ProximityLabel.WEAK
        else -> ProximityLabel.VERY_WEAK
    }
}

object RssiSampleWindow {
    fun retainRecent(
        samples: List<RssiSample>,
        nowMillis: Long,
        windowMillis: Long = SignalTrackerConfig.GRAPH_WINDOW_MILLIS,
        maxSamples: Int = SignalTrackerConfig.MAX_GRAPH_SAMPLES,
    ): List<RssiSample> {
        val cutoff = nowMillis - windowMillis
        return samples.asSequence()
            .filter { it.timestamp >= cutoff }
            .toList()
            .takeLast(maxSamples)
    }

    fun retainRecentSmoothed(
        samples: List<SmoothedRssiSample>,
        nowMillis: Long,
        windowMillis: Long = SignalTrackerConfig.GRAPH_WINDOW_MILLIS,
        maxSamples: Int = SignalTrackerConfig.MAX_GRAPH_SAMPLES,
    ): List<SmoothedRssiSample> {
        val cutoff = nowMillis - windowMillis
        return samples.asSequence()
            .filter { it.timestamp >= cutoff }
            .toList()
            .takeLast(maxSamples)
    }
}
