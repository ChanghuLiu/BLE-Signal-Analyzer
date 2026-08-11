package com.ble.signal.analyzer.signal

import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class SignalStabilityLabel {
    COLLECTING,
    EXCELLENT,
    STABLE,
    VARIABLE,
    UNSTABLE,
}

data class SignalStabilityResult(
    val score: Int?,
    val label: SignalStabilityLabel,
    val standardDeviation: Double?,
    val rangeDb: Int?,
    val dropoutCount: Int,
    val suddenJumpCount: Int,
    val sampleCount: Int,
)

object SignalStabilityConfig {
    const val WINDOW_MILLIS = 30_000L
    const val MINIMUM_SAMPLE_COUNT = 6
    const val DROPOUT_GAP_MILLIS = 5_000L
    const val SUDDEN_JUMP_DB = 10.0
}

/**
 * App-defined relative stability metric. This is intentionally transparent and is not an
 * official Bluetooth standard measurement. It uses only recent, in-memory RSSI samples. Scores
 * start at 100: deviation above 2 dB and ranges above 6 dB add graduated penalties, each smoothed
 * jump over 10 dB subtracts 7 points (capped at 21), and each advertisement gap over 5 seconds
 * subtracts 10 points (capped at 30).
 */
object SignalStabilityCalculator {
    fun calculate(
        samples: List<RssiSample>,
        smoothedSamples: List<SmoothedRssiSample> = emptyList(),
        nowMillis: Long = samples.lastOrNull()?.timestamp ?: 0L,
    ): SignalStabilityResult {
        val windowStart = nowMillis - SignalStabilityConfig.WINDOW_MILLIS
        val recentSamples = samples
            .asSequence()
            .filter { it.timestamp in windowStart..nowMillis }
            .sortedBy { it.timestamp }
            .toList()
        val values = recentSamples.map { it.rssi.toDouble() }
        val standardDeviation = values.standardDeviationOrNull()
        val rangeDb = if (recentSamples.isEmpty()) {
            null
        } else {
            recentSamples.maxOf { it.rssi } - recentSamples.minOf { it.rssi }
        }
        val dropoutCount = countDropouts(recentSamples, nowMillis)
        val recentSmoothed = if (smoothedSamples.isEmpty()) {
            smooth(recentSamples)
        } else {
            smoothedSamples
                .asSequence()
                .filter { it.timestamp in windowStart..nowMillis }
                .sortedBy { it.timestamp }
                .toList()
        }
        val suddenJumpCount = recentSmoothed.zipWithNext().count { (first, second) ->
            kotlin.math.abs(second.rssi - first.rssi) > SignalStabilityConfig.SUDDEN_JUMP_DB
        }

        if (recentSamples.size < SignalStabilityConfig.MINIMUM_SAMPLE_COUNT) {
            return SignalStabilityResult(
                score = null,
                label = SignalStabilityLabel.COLLECTING,
                standardDeviation = standardDeviation,
                rangeDb = rangeDb,
                dropoutCount = dropoutCount,
                suddenJumpCount = suddenJumpCount,
                sampleCount = recentSamples.size,
            )
        }

        val deviationPenalty = standardDeviationPenalty(standardDeviation ?: 0.0)
        val rangePenalty = rangePenalty(rangeDb ?: 0)
        val jumpPenalty = (suddenJumpCount * 7).coerceAtMost(21)
        val dropoutPenalty = (dropoutCount * 10).coerceAtMost(30)
        val score = (100 - deviationPenalty - rangePenalty - jumpPenalty - dropoutPenalty)
            .coerceIn(0, 100)

        return SignalStabilityResult(
            score = score,
            label = labelFor(score),
            standardDeviation = standardDeviation,
            rangeDb = rangeDb,
            dropoutCount = dropoutCount,
            suddenJumpCount = suddenJumpCount,
            sampleCount = recentSamples.size,
        )
    }

    fun labelFor(score: Int): SignalStabilityLabel = when (score.coerceIn(0, 100)) {
        in 90..100 -> SignalStabilityLabel.EXCELLENT
        in 75..89 -> SignalStabilityLabel.STABLE
        in 50..74 -> SignalStabilityLabel.VARIABLE
        else -> SignalStabilityLabel.UNSTABLE
    }

    private fun standardDeviationPenalty(deviation: Double): Int = when {
        deviation <= 2.0 -> 0
        deviation <= 5.0 -> ((deviation - 2.0) * 3.0).roundToInt()
        deviation <= 10.0 -> (9.0 + (deviation - 5.0) * 4.0).roundToInt()
        else -> (29.0 + (deviation - 10.0) * 2.0).roundToInt().coerceAtMost(45)
    }

    private fun rangePenalty(rangeDb: Int): Int = when {
        rangeDb <= 6 -> 0
        rangeDb <= 15 -> ((rangeDb - 6) * 1.5).roundToInt()
        else -> (14.0 + (rangeDb - 15) * 0.8).roundToInt().coerceAtMost(30)
    }

    private fun countDropouts(samples: List<RssiSample>, nowMillis: Long): Int {
        if (samples.isEmpty()) return 0
        val internalDropouts = samples.zipWithNext().count { (first, second) ->
            second.timestamp - first.timestamp > SignalStabilityConfig.DROPOUT_GAP_MILLIS
        }
        val trailingDropout = if (
            nowMillis - samples.last().timestamp > SignalStabilityConfig.DROPOUT_GAP_MILLIS
        ) {
            1
        } else {
            0
        }
        return internalDropouts + trailingDropout
    }

    private fun smooth(samples: List<RssiSample>): List<SmoothedRssiSample> {
        var previous: Double? = null
        return samples.map { sample ->
            val smoothed = RssiSmoother.next(previous, sample.rssi)
            previous = smoothed
            SmoothedRssiSample(sample.timestamp, smoothed)
        }
    }
}

private fun List<Double>.standardDeviationOrNull(): Double? {
    if (isEmpty()) return null
    val mean = average()
    val variance = sumOf { value ->
        val difference = value - mean
        difference * difference
    } / size
    return sqrt(variance)
}
