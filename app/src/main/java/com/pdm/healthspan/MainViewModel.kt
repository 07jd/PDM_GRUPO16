package com.pdm.healthspan

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.deleteRecords
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.healthspan.ui.home.HomeEvent
import com.pdm.healthspan.ui.states.BiometricsState
import com.pdm.healthspan.ui.states.BiometricsType
import com.pdm.healthspan.ui.states.SleepStages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class MainViewModel: ViewModel() {
    /*
     * HEALTH CONNECT
     */

    // Null = not available on device, has to download (ex.: android < 13)
    private var healthConnect: HealthConnectClient? = null
    private val _biometricsUiState  = MutableStateFlow(BiometricsState())
    val biometricsUiState   = _biometricsUiState.asStateFlow()

    // Use on UI to ask for permissions
    val desiredPermissions  = setOf(
        HealthPermission.getReadPermission<HeartRateRecord>(),
        HealthPermission.getReadPermission<HeartRateVariabilityRmssdRecord>(),
        HealthPermission.getReadPermission<OxygenSaturationRecord>(),
        HealthPermission.getReadPermission<SleepSessionRecord>(),
        HealthPermission.getReadPermission<RespiratoryRateRecord>(),
        HealthPermission.getReadPermission<StepsRecord>(),
        HealthPermission.getReadPermission<ExerciseSessionRecord>(),
        HealthPermission.getWritePermission<ExerciseSessionRecord>()
    )

    // Functions
    private suspend fun readRecords(
        type: BiometricsType,
        range: TimeRangeFilter = TimeRangeFilter.after(
            LocalDateTime
                .now()
                .minusHours(24)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
    ) {
        try {
            val client = healthConnect!!
            when(type) {
                BiometricsType.HEART_RATE -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<HeartRateRecord>(range)
                    ).records

                    _biometricsUiState.update {
                        it.copy(
                            heartRate = raw.mapNotNull { record ->
                                val avg = record.samples
                                    .takeIf { samples -> samples.isNotEmpty() }
                                    ?.map { sample -> sample.beatsPerMinute }
                                    ?.average()

                                avg?.let { bpmAvg -> record.endTime.toKotlinInstant() to bpmAvg.toFloat() }
                            }
                        )
                    }
                }
                BiometricsType.RPM -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<RespiratoryRateRecord>(range)
                    ).records

                    _biometricsUiState.update {
                        it.copy(
                            rpm = raw.map { record ->
                                record.time.toKotlinInstant() to record.rate.toFloat()
                            }
                        )
                    }
                }
                BiometricsType.SPO2 -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<OxygenSaturationRecord>(range)
                    ).records

                    _biometricsUiState.update {
                        it.copy(
                            spo2 = raw.map { record ->
                                record.time.toKotlinInstant() to record.percentage.value.toFloat()
                            }
                        )
                    }
                }
                BiometricsType.SLEEP -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<SleepSessionRecord>(range)
                    ).records

                    val latestSession = raw.maxByOrNull { it.endTime }
                    _biometricsUiState.update {
                        it.copy(
                            sleep = latestSession?.stages?.map { stage ->
                                    Triple(
                                        stage.startTime.toKotlinInstant(),
                                        stage.endTime.toKotlinInstant(),
                                        when(stage.stage) {
                                            SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStages.AWAKE
                                            SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStages.DEEP
                                            SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStages.LIGHT
                                            else -> SleepStages.UNKNOWN
                                        }
                                    )
                                } ?: emptyList()
                        )
                    }
                }
                BiometricsType.STEPS -> {
                    val monthRange = TimeRangeFilter.after(
                        LocalDateTime
                            .now()
                            .minusDays(30)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                    )

                    val raw = client.readRecords(
                        ReadRecordsRequest<StepsRecord>(monthRange)
                    ).records

                    val today = LocalDate.now()

                    val stepsByDate: Map<LocalDate, Long> = raw.groupBy { record ->
                        record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                    }.mapValues { entry ->
                        entry.value.sumOf { it.count }
                    }

                    val monthlyList = (0..29).map { daysBack ->
                        val date = today.minusDays((29 - daysBack).toLong())
                        date to (stepsByDate[date] ?: 0L)
                    }

                    val weeklyList = (0..6).map { daysBack ->
                        val date = today.minusDays((6 - daysBack).toLong())
                        date to (stepsByDate[date] ?: 0L)
                    }

                    val hcTodaySteps = stepsByDate[today] ?: 0L
                    val liveSensor = _biometricsUiState.value.liveSensorSteps
                    val totalTodaySteps = maxOf(hcTodaySteps, liveSensor)

                    _biometricsUiState.update {
                        it.copy(
                            steps = raw.map { record ->
                                Triple(
                                    record.startTime.toKotlinInstant(),
                                    record.endTime.toKotlinInstant(),
                                    record.count
                                )
                            },
                            todaySteps = totalTodaySteps,
                            weeklySteps = weeklyList,
                            monthlySteps = monthlyList
                        )
                    }
                }
                BiometricsType.RMSSD -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<HeartRateVariabilityRmssdRecord>(range)
                    ).records


                    _biometricsUiState.update {
                        it.copy(
                            rmssd = raw.map { record ->
                                record.time.toKotlinInstant() to record.heartRateVariabilityMillis.toFloat()
                            }
                        )
                    }
                }
                BiometricsType.EXERCISES -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<ExerciseSessionRecord>(range)
                    ).records

                    println("EXERCICIOS: $raw")
                    _biometricsUiState.update { it.copy(exercices = raw) }
                }
            }
        } catch (e: Exception) {
            println("[readRecords] Failed to read records, reason: ${e.message}")
        }
    }

    private suspend fun getDataFromHealthConnect() {
        // Read data
        BiometricsType.entries.forEach { readRecords(it) }
    }

    // HOME UI Events
    fun onHomeEvent(event: HomeEvent) {
        viewModelScope.launch {
            when(event) {
                is HomeEvent.NewHealthConnectClient -> {
                    healthConnect = event.client
                    if(healthConnect != null) {
                        getDataFromHealthConnect()
                        _biometricsUiState.update {
                            it.copy(
                                validHealthConnectClient = true,
                                grantedPermissions = healthConnect!!.permissionController.getGrantedPermissions()
                            )
                        }
                    } else {
                        _biometricsUiState.update { it.copy(validHealthConnectClient = false) }
                    }
                }
                HomeEvent.Refresh                   -> {
                    _biometricsUiState.update { it.copy(refreshing = true) }
                    getDataFromHealthConnect()
                    _biometricsUiState.update { it.copy(refreshing = false) }
                }
                HomeEvent.RefreshGrantedPermissions -> {
                    if (healthConnect == null) {
                        _biometricsUiState.update { it.copy(grantedPermissions = emptySet()) }
                    } else {
                        getDataFromHealthConnect()
                        _biometricsUiState.update {
                            it.copy(
                                grantedPermissions = healthConnect!!.permissionController.getGrantedPermissions()
                            )
                        }
                    }
                }
                is HomeEvent.CreateExercise -> {
                    try {
                        val client = healthConnect!!
                        client.insertRecords(
                            listOf(
                                ExerciseSessionRecord(
                                    startTime = event.data.first.toJavaInstant(),
                                    startZoneOffset = null,
                                    endTime = event.data.second.toJavaInstant(),
                                    endZoneOffset = null,
                                    exerciseType = event.data.third,
                                    metadata = Metadata.manualEntry(device = null)
                                )
                            )
                        )
                    } catch (e: Exception) {
                        println("Failed to create exercise: ${e.message}")
                    } finally {
                        getDataFromHealthConnect()
                    }
                }
                is HomeEvent.DeleteExercise -> {
                    try {
                        val client = healthConnect!!
                        client.deleteRecords<ExerciseSessionRecord>(
                            recordIdsList = listOf(event.id),
                            clientRecordIdsList = emptyList()
                        )
                    } catch (e: Exception) {
                        println("Failed to delete exercise with id ${event.id}, reason : ${e.message}")
                    } finally {
                        getDataFromHealthConnect()
                    }
                }
            }
        }
    }

    fun updateLiveSensorSteps(sensorSteps: Long, isAvailable: Boolean) {
        _biometricsUiState.update { currentState ->
            val totalToday = maxOf(currentState.todaySteps, sensorSteps)
            currentState.copy(
                liveSensorSteps = sensorSteps,
                todaySteps = totalToday,
                isSensorAvailable = isAvailable
            )
        }
    }
}