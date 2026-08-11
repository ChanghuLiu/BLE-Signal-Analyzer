package com.ble.signal.analyzer.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityAlertEvaluatorTest {
    @Test
    fun weakerToStrongerCrossing_triggersOnce() {
        val result = evaluateSequence(
            values = listOf(-60.0, -55.0, -49.0),
            startTimeMillis = 0,
        )

        assertEquals(1, result.triggerCount)
        assertEquals(ProximityAlertStatus.WAITING_FOR_REARM, result.state.status)
    }

    @Test
    fun remainingAboveThreshold_doesNotTriggerAgain() {
        val first = evaluateSequence(
            values = listOf(-60.0, -49.0),
            startTimeMillis = 0,
        )
        val second = evaluateSequence(
            values = listOf(-47.0, -45.0),
            startTimeMillis = 5_000,
            initialState = first.state,
        )

        assertEquals(1, first.triggerCount)
        assertEquals(0, second.triggerCount)
    }

    @Test
    fun fallingBelowHysteresis_rearmsAndAllowsSecondAlert() {
        val first = evaluateSequence(
            values = listOf(-60.0, -49.0),
            startTimeMillis = 0,
        )
        val second = evaluateSequence(
            values = listOf(-56.0, -48.0),
            startTimeMillis = 6_000,
            initialState = first.state,
        )

        assertEquals(1, second.triggerCount)
        assertEquals(ProximityAlertStatus.WAITING_FOR_REARM, second.state.status)
    }

    @Test
    fun smallFluctuationsWithoutHysteresis_doNotSpam() {
        val first = evaluateSequence(
            values = listOf(-60.0, -49.0),
            startTimeMillis = 0,
        )
        val fluctuations = evaluateSequence(
            values = listOf(-51.0, -49.0, -52.0, -48.0, -54.9, -49.0),
            startTimeMillis = 6_000,
            initialState = first.state,
        )

        assertEquals(0, fluctuations.triggerCount)
        assertEquals(ProximityAlertStatus.WAITING_FOR_REARM, fluctuations.state.status)
    }

    @Test
    fun disabledAlert_neverTriggers() {
        var state = ProximityAlertEvaluationState()
        listOf(-60.0, -55.0, -49.0).forEachIndexed { index, rssi ->
            val evaluation = ProximityAlertEvaluator.evaluate(
                state = state,
                enabled = false,
                smoothedRssi = rssi,
                alertThreshold = -50,
                timestampMillis = index * 1_000L,
            )
            assertFalse(evaluation.shouldVibrate)
            state = evaluation.state
        }

        assertEquals(ProximityAlertStatus.DISABLED, state.status)
    }

    @Test
    fun firstSampleAboveThreshold_establishesBaselineWithoutAlert() {
        val evaluation = ProximityAlertEvaluator.evaluate(
            state = ProximityAlertEvaluator.enabledInitialState(),
            enabled = true,
            smoothedRssi = -49.0,
            alertThreshold = -50,
            timestampMillis = 0,
        )

        assertFalse(evaluation.shouldVibrate)
        assertEquals(ProximityAlertStatus.WAITING_FOR_REARM, evaluation.state.status)
    }

    @Test
    fun cooldown_blocksRapidSecondCrossingEvenAfterRearm() {
        val first = evaluateSequence(
            values = listOf(-60.0, -49.0),
            startTimeMillis = 0,
            intervalMillis = 1_000,
        )
        val rapidSecond = evaluateSequence(
            values = listOf(-56.0, -48.0),
            startTimeMillis = 2_000,
            intervalMillis = 1_000,
            initialState = first.state,
        )

        assertEquals(0, rapidSecond.triggerCount)
        assertEquals(1_000L, rapidSecond.state.lastTriggeredAtMillis)
    }

    @Test
    fun rearmBoundary_isThresholdMinusFiveDbm() {
        val triggered = evaluateSequence(listOf(-60.0, -49.0), startTimeMillis = 0)
        val rearmed = ProximityAlertEvaluator.evaluate(
            state = triggered.state,
            enabled = true,
            smoothedRssi = -55.0,
            alertThreshold = -50,
            timestampMillis = 6_000,
        )

        assertFalse(rearmed.shouldVibrate)
        assertEquals(ProximityAlertStatus.READY, rearmed.state.status)
    }

    private fun evaluateSequence(
        values: List<Double>,
        startTimeMillis: Long,
        intervalMillis: Long = 1_000,
        initialState: ProximityAlertEvaluationState =
            ProximityAlertEvaluator.enabledInitialState(),
    ): SequenceResult {
        var state = initialState
        var triggerCount = 0
        values.forEachIndexed { index, rssi ->
            val evaluation = ProximityAlertEvaluator.evaluate(
                state = state,
                enabled = true,
                smoothedRssi = rssi,
                alertThreshold = -50,
                timestampMillis = startTimeMillis + index * intervalMillis,
            )
            if (evaluation.shouldVibrate) triggerCount += 1
            state = evaluation.state
        }
        assertTrue(values.isNotEmpty())
        return SequenceResult(state, triggerCount)
    }

    private data class SequenceResult(
        val state: ProximityAlertEvaluationState,
        val triggerCount: Int,
    )
}
