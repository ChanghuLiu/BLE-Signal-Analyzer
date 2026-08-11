package com.ble.signal.analyzer.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalStabilityCalculatorTest {
    @Test
    fun stableReadingsProduceHighScore() {
        val result = calculate(listOf(-51, -52, -50, -51, -52, -51))

        assertNotNull(result.score)
        assertTrue(result.score!! >= 90)
        assertEquals(SignalStabilityLabel.EXCELLENT, result.label)
    }

    @Test
    fun variableReadingsProduceSubstantiallyLowerScore() {
        val stable = calculate(listOf(-51, -52, -50, -51, -52, -51))
        val variable = calculate(listOf(-45, -70, -52, -78, -48, -69))

        assertNotNull(variable.score)
        assertTrue(variable.score!! <= stable.score!! - 30)
    }

    @Test
    fun insufficientSamplesReturnCollectingWithoutScore() {
        val result = calculate(listOf(-51, -52, -50, -51, -52))

        assertNull(result.score)
        assertEquals(SignalStabilityLabel.COLLECTING, result.label)
        assertEquals(5, result.sampleCount)
    }

    @Test
    fun rangeUsesWeakestAndStrongestReadings() {
        val result = calculate(listOf(-60, -55, -65, -58, -62, -61))

        assertEquals(10, result.rangeDb)
    }

    @Test
    fun standardDeviationUsesPopulationCalculation() {
        val result = calculate(listOf(-50, -50, -50, -54, -54, -54))

        assertEquals(2.0, result.standardDeviation!!, 0.0001)
    }

    @Test
    fun scoreIsAlwaysClampedToZeroThroughOneHundred() {
        val raw = listOf(-30, -100, -30, -100, -30, -100).toSamples(gapMillis = 6_000L)
        val smoothed = raw.mapIndexed { index, sample ->
            SmoothedRssiSample(sample.timestamp, if (index % 2 == 0) -30.0 else -100.0)
        }
        val result = SignalStabilityCalculator.calculate(
            samples = raw,
            smoothedSamples = smoothed,
            nowMillis = raw.last().timestamp,
        )

        assertTrue(result.score!! in 0..100)
        assertEquals(0, result.score)
    }

    @Test
    fun dropoutPeriodReducesScore() {
        val continuous = calculate(listOf(-50, -50, -50, -50, -50, -50))
        val withDropoutSamples = listOf(-50, -50, -50, -50, -50, -50)
            .toSamples(gapMillis = 1_000L)
            .mapIndexed { index, sample ->
                if (index >= 3) sample.copy(timestamp = sample.timestamp + 6_000L) else sample
            }
        val withDropout = SignalStabilityCalculator.calculate(
            samples = withDropoutSamples,
            nowMillis = withDropoutSamples.last().timestamp,
        )

        assertEquals(1, withDropout.dropoutCount)
        assertTrue(withDropout.score!! < continuous.score!!)
    }

    @Test
    fun repeatedSuddenSmoothedJumpsReduceScore() {
        val raw = listOf(-50, -50, -50, -50, -50, -50).toSamples()
        val smoothed = listOf(-50.0, -50.0, -65.0, -50.0, -65.0, -50.0)
            .mapIndexed { index, value -> SmoothedRssiSample(raw[index].timestamp, value) }
        val result = SignalStabilityCalculator.calculate(
            samples = raw,
            smoothedSamples = smoothed,
            nowMillis = raw.last().timestamp,
        )

        assertEquals(4, result.suddenJumpCount)
        assertTrue(result.score!! < 100)
    }

    private fun calculate(values: List<Int>): SignalStabilityResult {
        val samples = values.toSamples()
        return SignalStabilityCalculator.calculate(samples, nowMillis = samples.last().timestamp)
    }

    private fun List<Int>.toSamples(gapMillis: Long = 1_000L): List<RssiSample> =
        mapIndexed { index, rssi -> RssiSample(index * gapMillis, rssi) }
}
