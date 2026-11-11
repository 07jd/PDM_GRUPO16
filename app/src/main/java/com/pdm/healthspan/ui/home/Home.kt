package com.pdm.healthspan.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.ChartEmptyState
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.LineInterpolation
import com.himanshoe.charty.common.config.PersistentMarker
import com.himanshoe.charty.common.config.ReferenceLineConfig
import com.himanshoe.charty.line.AreaChart
import com.himanshoe.charty.line.config.LineChartConfig
import com.himanshoe.charty.line.data.LineData
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant


@Composable
fun Home() {
    val context = LocalContext.current

    // Perms
    var grantedPermissions by remember { mutableStateOf(emptySet<String>()) }
    val permissions = setOf(
        HealthPermission.getReadPermission<HeartRateRecord>(),
        HealthPermission.getReadPermission<HeartRateVariabilityRmssdRecord>(),
        HealthPermission.getReadPermission<OxygenSaturationRecord>(),
        HealthPermission.getReadPermission<SleepSessionRecord>(),
        HealthPermission.getReadPermission<RespiratoryRateRecord>(),
        HealthPermission.getReadPermission<StepsRecord>(),
        HealthPermission.getReadPermission<PlannedExerciseSessionRecord>(),
        HealthPermission.getWritePermission<PlannedExerciseSessionRecord>()
    )

    // Launch activity
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { userGivenPerms -> grantedPermissions = userGivenPerms }

    val availabilityStatus = HealthConnectClient.getSdkStatus(context)
    if(availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
        Text(text = "HealthConnectClient returned code $availabilityStatus (HealthConnectClient.SDK_AVAILABLE)")
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            enabled = (grantedPermissions.size - permissions.size) != 0,
            modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp),
            onClick = {
                try {
                    println("Launching perms intent")
                    requestPermissionLauncher.launch(permissions)
                } catch(e: Exception) {
                    println("Failed due to ${e.message}")
                }
            }
        ) {
            val qntPerms = (permissions.size - grantedPermissions.size)

            Text(
                text = if(qntPerms == 0) "ALL PERMS GIVEN"
                else "NOT ALL PERMS GIVEN CHECK LOGCAT [Missing: ${qntPerms}]"
            )
        }



        val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
        val scope = rememberCoroutineScope()
        var displayData: List<Pair<Instant, Float>>? by remember { mutableStateOf(null) }

        // Fill displayData
        suspend fun populateData() {
            println("getting data")
            val data = healthConnectClient.readRecords(
                ReadRecordsRequest<HeartRateRecord>(
                    timeRangeFilter = TimeRangeFilter.after(
                        LocalDateTime
                                .now()
                                .minusHours(24)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                    )
                )
            )
            println("got data ${data.records}")

            displayData = data.records.map {
                val avg = it.samples.sumOf { bpm -> bpm.beatsPerMinute } / it.samples.size.toFloat()
                it.endTime.toKotlinInstant() to avg
            }
        }

        // On init get granted perms
        LaunchedEffect(Unit) {
            grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        }

        // Auto populate data
        LaunchedEffect(grantedPermissions) {
            if (grantedPermissions.contains(HealthPermission.getReadPermission<HeartRateRecord>())) {
                scope.launch { populateData() }
            }
        }


        Button(
            enabled = grantedPermissions.contains(
                HealthPermission.getReadPermission<HeartRateRecord>()
            ),
            onClick = {
                scope.launch { scope.launch { populateData() } }
            }
        ) { Text("REFRESH DATA")}

        HeartRateChart(displayData)
    }
}

@Composable
private fun HeartRateChart(
    data: List<Pair<Instant, Float>>?,
    height: Dp = 275.dp
) {
    val now = Clock.System.now()
    val values = data.orEmpty().mapIndexed { index, (time, bpm) ->
        val timeDif = now - time
        val relativeTime = if (timeDif.inWholeHours <= 0) { "${timeDif.inWholeMinutes} m" }
                           else { "${timeDif.inWholeMinutes} h" }

        LineData(
            label = when {
                index == 0 ||
                index == data?.size?.div(2) ||
                index == data?.size
                                            -> relativeTime
                else                        -> ""
            },
            value = bpm.roundToInt().toFloat()
        )
    }

    val max = values.maxOfOrNull { it.value }
    val min  = values.minOfOrNull { it.value }

    val bottom  = values.find { it.value == min }
    val peak    = values.find { it.value == max }

    val markers = buildList {
        if(bottom != null) add(PersistentMarker(
            label = "Slowest ${bottom.value} BPM",
            dataIndex = values.indexOf(bottom)
        ))
        if(peak != null) add(PersistentMarker(
            label = "Fastest ${peak.value} BPM",
            dataIndex = values.indexOf(peak)
        ))
    }

    val avgBPM = if(values.isNotEmpty()) values.map { it.value }.average().roundToInt().toFloat() else null
    val avgReferenceLine = if (avgBPM != null) ReferenceLineConfig(
        value = avgBPM,
        label = "Avg BPM $avgBPM",
        color = ChartyColor.Solid(Color.White),
        labelTextColor = ChartyColor.Solid(Color.White)
    ) else null

    Box(
        modifier = Modifier
                .fillMaxWidth()
                .height(height),
        contentAlignment = Alignment.Center
    ) {
        AreaChart(
            emptyContent = { ChartEmptyState { Text("No data") } },
            data = { values },
            modifier = Modifier.fillMaxSize(),
            color = ChartyColor.Gradient(listOf(Color.Red, Color.Magenta)),
            lineConfig = LineChartConfig(
                lineWidth = 2f,
                showPoints = false,
                interpolation = LineInterpolation.SMOOTH,
                fillAlpha = 0.35f,
                animation = Animation.Smooth,
                markers = markers,
                referenceLine = avgReferenceLine
            ),
        )
    }

}