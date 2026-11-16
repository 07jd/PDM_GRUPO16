package com.pdm.healthspan.ui.components

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.himanshoe.charty.block.BlockBarChart
import com.himanshoe.charty.block.config.BlockBarChartConfig
import com.himanshoe.charty.block.data.BlockData
import com.himanshoe.charty.color.ChartyColor
import com.himanshoe.charty.common.config.Animation
import com.himanshoe.charty.common.config.CornerRadius
import com.himanshoe.charty.common.config.LineInterpolation
import com.himanshoe.charty.common.config.PersistentMarker
import com.himanshoe.charty.common.config.ReferenceLineConfig
import com.himanshoe.charty.line.AreaChart
import com.himanshoe.charty.line.config.LineChartConfig
import com.himanshoe.charty.line.data.LineData
import com.pdm.healthspan.R
import com.pdm.healthspan.ui.states.BiometricsState
import com.pdm.healthspan.ui.states.BiometricsType
import com.pdm.healthspan.ui.states.SleepData
import com.pdm.healthspan.ui.states.SleepStages
import com.pdm.healthspan.ui.states.toStringRes
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 7)
@Composable
fun BiometricCharts(
    data: BiometricsState,
    height: Dp = 375.dp
) {
    var currentChartType by rememberSaveable { mutableStateOf(BiometricsType.HEART_RATE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Buttons and label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Back button
            TextButton(
                onClick = {
                    val idx = if(currentChartType.ordinal - 1 < 0) {
                        BiometricsType.entries.size - 2
                    } else { currentChartType.ordinal - 1 }

                    currentChartType = BiometricsType.entries[idx] }
            ) { Text(text = stringResource(R.string.back)) }

            // Label
            Text(text = stringResource(currentChartType.toStringRes()))

            // Next button
            TextButton(
                onClick = {
                    val new = BiometricsType.entries[(currentChartType.ordinal + 1) % BiometricsType.entries.size]
                    currentChartType =
                        if(new == BiometricsType.EXERCISES) BiometricsType.entries[0] else new
                }
            ) { Text(text = stringResource(R.string.next)) }
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        AnimatedContent(
            targetState = currentChartType,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) { 1 } else { -1 }
                slideInHorizontally { it * direction } togetherWith slideOutHorizontally { it * -direction }
            }
        ) { type ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when(type) {
                    BiometricsType.HEART_RATE  -> {
                        if(data.heartRate == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            LineChart(
                                data = data.heartRate,
                                unit = "bpm",
                                topColor = Color.Magenta,
                                bottomColor = Color.Red
                            )
                        }
                    }
                    BiometricsType.RPM -> {
                        if(data.rpm == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            LineChart(data = data.rpm, unit = "")
                        }
                    }
                    BiometricsType.SPO2 -> {
                        if(data.spo2 == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            LineChart(
                                data = data.spo2,
                                unit = "%",
                                showAverage = false,
                                bottomColor = Color.Blue,
                                topColor = Color.Cyan
                            )
                        }
                    }
                    BiometricsType.SLEEP -> {
                        if(data.sleep == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            SleepChart(data = data.sleep)
                        }
                    }
                    BiometricsType.STEPS -> {
                        val stepData = data.steps?.flatMap {
                            listOf(
                                it.first to it.third.toFloat(),
                                it.second to it.third.toFloat()
                            )
                        }

                        if(stepData == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            LineChart(
                                data = stepData,
                                unit = "steps",
                                bottomColor = Color.Green,
                                topColor = Color.DarkGray
                            )
                        }
                    }
                    BiometricsType.RMSSD -> {
                        if(data.rmssd == null) {
                            Text(text = stringResource(R.string.no_permission))
                        } else {
                            LineChart(data = data.rmssd, unit = "ms")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<Pair<Instant, Float>>,
    showAverage: Boolean = true,
    unit: String? = null,
    topColor: Color = MaterialTheme.colorScheme.secondary,
    bottomColor: Color = MaterialTheme.colorScheme.primary,
    referenceLine: Color = MaterialTheme.colorScheme.tertiary,
) {
    val now = Clock.System.now()
    val chartData = remember(data) {
        data.mapIndexed { index, (time, bpm) ->
            LineData(
                label = when {
                    index == 0 ||
                    index == data.size.div(2) ||
                    index == data.size -> {
                        val timeDif = now - time

                        // Show in hours/minutes
                        if (timeDif.inWholeHours <= 0) "${timeDif.inWholeMinutes}m"
                        else "${timeDif.inWholeHours}h"
                    }
                    else -> ""
                },
                value = bpm.fastRoundToInt().toFloat()
            )
        }
    }

    // Min,Max,Avg
    val markersMinMax = remember(data) {
        val min = data.indexOf(data.minByOrNull { it.second })
        val max = data.indexOf(data.maxByOrNull { it.second })

        fun createMarker(index: Int): PersistentMarker {
            return PersistentMarker(
                label = "${data[index].second} ${unit ?: ""}",
                dataIndex = index
            )
        }

        buildList {
            if(min > 0) add(createMarker(min))
            if(max > 0) add(createMarker(max))
        }
    }

    val avgReferenceLine = remember(data) {
        val avg = if(data.isEmpty()) 0.0f else data.map { it.second }.average().roundToInt().toFloat()

        if(avg > 0 && showAverage) {
            ReferenceLineConfig(
                value = avg,
                label = "Average $avg",
                color = ChartyColor.Solid(referenceLine),
                labelTextColor = ChartyColor.Solid(referenceLine)
            )
        } else {
            null
        }
    }

    AreaChart(
        modifier = Modifier.fillMaxSize(),
        data = { chartData },
        color = ChartyColor.Gradient(
            listOf(
                topColor,
                bottomColor
            )
        ),
        lineConfig = LineChartConfig(
            lineWidth = 2f,
            showPoints = false,
            interpolation = LineInterpolation.SMOOTH,
            fillAlpha = 0.35f,
            animation = Animation.Smooth,
            markers = markersMinMax,
            referenceLine = avgReferenceLine
        )
    )
}

@Composable
private fun SleepChart(data: List<SleepData>) {
    val chartData = remember(data) {
        var awake       = 0.0
        var sleepDeep   = 0.0
        var sleepLight  = 0.0
        var unknown     = 0.0

        data.forEach { stage ->
            val time = stage.second.minus(stage.first).toLong(DurationUnit.SECONDS)
            when(stage.third) {
                SleepStages.UNKNOWN -> unknown += time
                SleepStages.AWAKE   -> awake += time
                SleepStages.DEEP    -> sleepDeep += time
                SleepStages.LIGHT   -> sleepLight += time
            }
        }

        val total = awake + sleepLight + sleepDeep + unknown

        awake       = awake.div(total)
        sleepDeep   = sleepDeep.div(total)
        sleepLight  = sleepLight.div(total)
        unknown     = unknown.div(total)

        listOf(
            awake.toFloat(),
            sleepDeep.toFloat(),
            sleepLight.toFloat(),
            unknown.toFloat()
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BlockBarChart(
            modifier = Modifier.fillMaxWidth(),
            data = {
                buildList{
                    add(BlockData(chartData[0], ChartyColor.Solid(Color.Blue)))
                    add(BlockData(chartData[1], ChartyColor.Solid(Color.Cyan)))
                    add(BlockData(chartData[2], ChartyColor.Solid(Color.Yellow)))
                    add(BlockData(chartData[3], ChartyColor.Solid(Color.Gray)))
                }
            },
            blockBarConfig = BlockBarChartConfig(
                cornerRadius = CornerRadius.Small,
                gapBetweenBlocks = 10.dp,
                barHeight = 50.dp,
            )
        )

        // Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SleepStages.entries.forEach {
                val color = when(it) {
                    SleepStages.UNKNOWN -> Color.Gray
                    SleepStages.AWAKE   -> Color.Yellow
                    SleepStages.DEEP    -> Color.Blue
                    SleepStages.LIGHT   -> Color.Cyan
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).background(color))
                    Text(it.name)
                }
            }
        }

        // Duration label
        val duration = data.firstOrNull()?.let {
            val duration = it.second.minus(it.first).inWholeMinutes

            if(duration >= 60) {
                val minutes = duration % 60
                val hours = duration / 60

                "$hours:$minutes h"
            }else {
                "$duration m"
            }
        }

        if(duration != null) {
            Text(text = "Duration: $duration")
        }
    }
}