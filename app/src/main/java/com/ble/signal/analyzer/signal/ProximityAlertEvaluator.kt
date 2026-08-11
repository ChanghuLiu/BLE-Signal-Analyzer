package com.ble.signal.analyzer.signal

enum class ProximityAlertStatus {
    DISABLED,
    OBSERVING,
    READY,
    WAITING_FOR_REARM,
}

data class ProximityAlertEvaluationState(
    val status: ProximityAlertStatus = ProximityAlertStatus.DISABLED,
    val previousSmoothedRssi: Double? = null,
    val lastTriggeredAtMillis: Long? = null,
)

data class ProximityAlertEvaluation(
    val state: ProximityAlertEvaluationState,
    val shouldVibrate: Boolean,
)

/**
 * Evaluates smoothed RSSI crossings without Android dependencies.
 *
 * The first enabled sample establishes a baseline and never fires. After a trigger, the signal
 * must fall [SignalTrackerConfig.ALERT_HYSTERESIS_DBM] below the threshold before it can re-arm.
 */
object ProximityAlertEvaluator {
    fun evaluate(
        state: ProximityAlertEvaluationState,
        enabled: Boolean,
        smoothedRssi: Double,
        alertThreshold: Int,
        timestampMillis: Long,
    ): ProximityAlertEvaluation {
        val threshold = alertThreshold.toDouble()
        val rearmThreshold = threshold - SignalTrackerConfig.ALERT_HYSTERESIS_DBM

        if (!enabled) {
            return ProximityAlertEvaluation(
                state = state.copy(
                    status = ProximityAlertStatus.DISABLED,
                    previousSmoothedRssi = null,
                ),
                shouldVibrate = false,
            )
        }

        val previous = state.previousSmoothedRssi
        if (previous == null || state.status == ProximityAlertStatus.DISABLED) {
            val baselineStatus = if (smoothedRssi < threshold) {
                ProximityAlertStatus.READY
            } else {
                ProximityAlertStatus.WAITING_FOR_REARM
            }
            return ProximityAlertEvaluation(
                state = state.copy(
                    status = baselineStatus,
                    previousSmoothedRssi = smoothedRssi,
                ),
                shouldVibrate = false,
            )
        }

        if (state.status == ProximityAlertStatus.WAITING_FOR_REARM) {
            val nextStatus = if (smoothedRssi <= rearmThreshold) {
                ProximityAlertStatus.READY
            } else {
                ProximityAlertStatus.WAITING_FOR_REARM
            }
            return ProximityAlertEvaluation(
                state = state.copy(
                    status = nextStatus,
                    previousSmoothedRssi = smoothedRssi,
                ),
                shouldVibrate = false,
            )
        }

        val crossedThreshold = previous < threshold && smoothedRssi >= threshold
        if (!crossedThreshold) {
            return ProximityAlertEvaluation(
                state = state.copy(
                    status = ProximityAlertStatus.READY,
                    previousSmoothedRssi = smoothedRssi,
                ),
                shouldVibrate = false,
            )
        }

        val cooldownElapsed = state.lastTriggeredAtMillis?.let { lastTriggered ->
            timestampMillis - lastTriggered >= SignalTrackerConfig.ALERT_COOLDOWN_MILLIS
        } ?: true
        return ProximityAlertEvaluation(
            state = state.copy(
                status = ProximityAlertStatus.WAITING_FOR_REARM,
                previousSmoothedRssi = smoothedRssi,
                lastTriggeredAtMillis = if (cooldownElapsed) {
                    timestampMillis
                } else {
                    state.lastTriggeredAtMillis
                },
            ),
            shouldVibrate = cooldownElapsed,
        )
    }

    fun enabledInitialState(lastTriggeredAtMillis: Long? = null) =
        ProximityAlertEvaluationState(
            status = ProximityAlertStatus.OBSERVING,
            lastTriggeredAtMillis = lastTriggeredAtMillis,
        )
}
