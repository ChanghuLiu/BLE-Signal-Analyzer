package com.ble.signal.analyzer.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssiAnalysisTest {
    @Test
    fun ema_usesFirstValueAsInitialSmoothedValue() {
        assertEquals(-70.0, RssiSmoother.next(null, -70), 0.0001)
    }

    @Test
    fun ema_appliesFixedThirtyPercentWeightToSubsequentValue() {
        assertEquals(-64.0, RssiSmoother.next(-70.0, -50), 0.0001)
    }

    @Test
    fun trend_detectsClearlyIncreasingSignal() {
        val samples = trendSamples(
            previous = listOf(-75.0, -74.0),
            recent = listOf(-66.0, -65.0, -64.0),
        )

        assertEquals(
            SignalTrend.STRONGER,
            SignalTrendCalculator.calculate(samples, nowMillis = 5_000L),
        )
    }

    @Test
    fun trend_detectsClearlyDecreasingSignal() {
        val samples = trendSamples(
            previous = listOf(-55.0, -56.0),
            recent = listOf(-65.0, -66.0, -67.0),
        )

        assertEquals(
            SignalTrend.WEAKER,
            SignalTrendCalculator.calculate(samples, nowMillis = 5_000L),
        )
    }

    @Test
    fun trend_treatsSmallFluctuationsAsStable() {
        val samples = trendSamples(
            previous = listOf(-60.0, -61.0),
            recent = listOf(-59.5, -60.5, -60.0),
        )

        assertEquals(
            SignalTrend.STABLE,
            SignalTrendCalculator.calculate(samples, nowMillis = 5_000L),
        )
    }

    @Test
    fun trend_collectsUntilBothWindowsHaveEnoughSamples() {
        val samples = listOf(
            SmoothedRssiSample(1_000L, -70.0),
            SmoothedRssiSample(4_000L, -60.0),
        )

        assertEquals(
            SignalTrend.COLLECTING,
            SignalTrendCalculator.calculate(samples, nowMillis = 5_000L),
        )
    }

    @Test
    fun statistics_useWeakestAsMinAndStrongestAsMax() {
        val accumulator = SignalStatisticsAccumulator()
        accumulator.add(-70)
        accumulator.add(-60)
        val statistics = accumulator.add(-50)

        assertEquals(-70, statistics.min)
        assertEquals(-60, statistics.average)
        assertEquals(-50, statistics.max)
    }

    @Test
    fun emptyStatisticsHaveNoSnapshot() {
        assertNull(SignalStatisticsAccumulator().snapshot())
    }

    @Test
    fun proximityMapping_handlesAllBoundaries() {
        assertEquals(ProximityLabel.VERY_CLOSE, ProximityLabelMapper.fromSmoothedRssi(-45.0))
        assertEquals(ProximityLabel.CLOSE, ProximityLabelMapper.fromSmoothedRssi(-45.1))
        assertEquals(ProximityLabel.CLOSE, ProximityLabelMapper.fromSmoothedRssi(-59.0))
        assertEquals(ProximityLabel.NEARBY, ProximityLabelMapper.fromSmoothedRssi(-59.1))
        assertEquals(ProximityLabel.NEARBY, ProximityLabelMapper.fromSmoothedRssi(-69.0))
        assertEquals(ProximityLabel.WEAK, ProximityLabelMapper.fromSmoothedRssi(-69.1))
        assertEquals(ProximityLabel.WEAK, ProximityLabelMapper.fromSmoothedRssi(-79.0))
        assertEquals(ProximityLabel.VERY_WEAK, ProximityLabelMapper.fromSmoothedRssi(-79.1))
    }

    @Test
    fun rollingWindow_removesSamplesOlderThanThirtySeconds() {
        val samples = listOf(
            RssiSample(9_999L, -80),
            RssiSample(10_000L, -70),
            RssiSample(25_000L, -60),
            RssiSample(40_000L, -50),
        )

        assertEquals(
            listOf(
                RssiSample(10_000L, -70),
                RssiSample(25_000L, -60),
                RssiSample(40_000L, -50),
            ),
            RssiSampleWindow.retainRecent(samples, nowMillis = 40_000L),
        )
    }

    private fun trendSamples(
        previous: List<Double>,
        recent: List<Double>,
    ): List<SmoothedRssiSample> {
        val previousTimes = listOf(500L, 1_500L)
        val recentTimes = listOf(3_000L, 4_000L, 5_000L)
        return previous.mapIndexed { index, value ->
            SmoothedRssiSample(previousTimes[index], value)
        } + recent.mapIndexed { index, value ->
            SmoothedRssiSample(recentTimes[index], value)
        }
    }
}
