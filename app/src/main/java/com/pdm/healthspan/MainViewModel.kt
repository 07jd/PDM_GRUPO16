package com.pdm.healthspan

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.healthspan.ui.home.HomeEvent
import com.pdm.healthspan.ui.states.BiometricsState
import com.pdm.healthspan.ui.states.BiometricsType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
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
        HealthPermission.getReadPermission<PlannedExerciseSessionRecord>(),
        HealthPermission.getWritePermission<PlannedExerciseSessionRecord>()
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

                    _biometricsUiState.update {
                        it.copy(
                            sleep = raw.map { record ->
                                Triple(
                                    record.startTime.toKotlinInstant(),
                                    record.endTime.toKotlinInstant(),
                                    record.stages.map { stage ->
                                        Triple(
                                            stage.startTime.toKotlinInstant(),
                                            stage.endTime.toKotlinInstant(),
                                            stage.stage
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
                BiometricsType.STEPS -> {
                    val raw = client.readRecords(
                        ReadRecordsRequest<StepsRecord>(range)
                    ).records

                    _biometricsUiState.update {
                        it.copy(
                            steps = raw.map { record ->
                                Triple(
                                    record.startTime.toKotlinInstant(),
                                    record.endTime.toKotlinInstant(),
                                    record.count
                                )
                            }
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
            }
        }
    }
}