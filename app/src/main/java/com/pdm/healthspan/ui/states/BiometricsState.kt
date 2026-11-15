package com.pdm.healthspan.ui.states

import androidx.annotation.StringRes
import com.pdm.healthspan.R
import kotlin.time.Instant

enum class BiometricsType {
    HEART_RATE,
    RPM,
    SPO2,
    SLEEP,
    STEPS,
    RMSSD
}

@StringRes
fun BiometricsType.toStringRes(): Int = when(this) {
    BiometricsType.HEART_RATE -> R.string.heart_rate
    BiometricsType.RPM -> R.string.rpm
    BiometricsType.SPO2 -> R.string.spo2
    BiometricsType.SLEEP -> R.string.sleep
    BiometricsType.STEPS -> R.string.steps
    BiometricsType.RMSSD -> R.string.rmssd
}

typealias Data = Pair<Instant, Float>
// SLEEP_START, SLEEP_END, SLEEP_STAGES([START,END,STAGE_TYPE])
typealias SleepData = Triple<Instant, Instant, List<Triple<Instant, Instant, Int>>>
data class BiometricsState(
    val validHealthConnectClient: Boolean?                  = null,
    val refreshing: Boolean                                 = false,
    val grantedPermissions: Set<String>                     = emptySet(),

    // Data
    // Null = no permission to get data
    val heartRate  : List<Data>?                           = null,
    val rpm         : List<Data>?                           = null,
    val spo2        : List<Data>?                           = null,
    val sleep       : List<SleepData>?                      = null,
    val steps       : List<Triple<Instant, Instant, Long>>? = null,
    val rmssd       : List<Data>?                           = null
)