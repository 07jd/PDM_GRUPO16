package com.pdm.healthspan.ui.states

import androidx.annotation.StringRes
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.pdm.healthspan.R
import kotlin.time.Instant

enum class BiometricsType {
    HEART_RATE,
    RPM,
    SPO2,
    SLEEP,
    STEPS,
    RMSSD,
    EXERCISES
}

enum class SleepStages {
    UNKNOWN,
    AWAKE,
    DEEP,
    LIGHT
}

@StringRes
fun BiometricsType.toStringRes(): Int = when(this) {
    BiometricsType.HEART_RATE -> R.string.heart_rate
    BiometricsType.RPM -> R.string.rpm
    BiometricsType.SPO2 -> R.string.spo2
    BiometricsType.SLEEP -> R.string.sleep
    BiometricsType.STEPS -> R.string.steps
    BiometricsType.RMSSD -> R.string.rmssd
    BiometricsType.EXERCISES -> R.string.destination_exercises
}

typealias Data = Pair<Instant, Float>
// SLEEP_START, SLEEP_END, SLEEP_STAGES([START,END,STAGE_TYPE])
typealias SleepData = Triple<Instant, Instant, SleepStages>
data class BiometricsState(
    val validHealthConnectClient: Boolean?                  = null,
    val refreshing: Boolean                                 = false,
    val grantedPermissions: Set<String>                     = emptySet(),

    // Data
    // Null = no permission to get data
    val heartRate   : List<Data>?                           = null,
    val rpm         : List<Data>?                           = null,
    val spo2        : List<Data>?                           = null,
    val sleep       : List<SleepData>?                      = null,
    val steps       : List<Triple<Instant, Instant, Long>>? = null,
    val rmssd       : List<Data>?                           = null,
    val exercices   : List<ExerciseSessionRecord>?          = null
)