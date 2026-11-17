package com.pdm.healthspan.ui.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdm.healthspan.R
import com.pdm.healthspan.ui.states.BiometricsState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StepsScreen(
    uiState: BiometricsState,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Semana, 1 = Mês

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today Summary Card
        item {
            TodayStepCard(uiState = uiState)
        }

        // Tab Selector (Semana / Mês)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(text = stringResource(R.string.week), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(text = stringResource(R.string.month), fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Tab Content
        item {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "StepsTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> WeeklyStepsView(uiState = uiState)
                    1 -> MonthlyStepsView(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun TodayStepCard(uiState: BiometricsState) {
    val progress = (uiState.todaySteps.toFloat() / uiState.stepGoal).coerceIn(0f, 1f)
    val distanceKm = String.format(Locale.getDefault(), "%.2f", uiState.todaySteps * 0.00075f)
    val caloriesKcal = (uiState.todaySteps * 0.04f).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_footprints_fill),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.steps_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sensor indicator badge
                Box(
                    modifier = Modifier
                        .background(
                            if (uiState.isSensorAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isSensorAvailable) stringResource(R.string.sensor_active)
                        else stringResource(R.string.sensor_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Big Step Count
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%,d", uiState.todaySteps),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "/ ${String.format(Locale.getDefault(), "%,d", uiState.stepGoal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            // Extra Stats (Distance & Calories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$distanceKm km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.distance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$caloriesKcal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.calories),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStepsView(uiState: BiometricsState) {
    val weekly = uiState.weeklySteps
    val totalWeekly = weekly.sumOf { it.second }
    val averageWeekly = if (weekly.isNotEmpty()) totalWeekly / weekly.size else 0L
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                title = stringResource(R.string.total_steps),
                value = String.format(Locale.getDefault(), "%,d", totalWeekly),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = stringResource(R.string.average_daily),
                value = String.format(Locale.getDefault(), "%,d", averageWeekly),
                modifier = Modifier.weight(1f)
            )
        }

        // Custom Bar Chart for 7 Days
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.weekly_steps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val maxSteps = (weekly.maxOfOrNull { it.second } ?: 10000L).coerceAtLeast(10000L)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekly.forEach { (date, count) ->
                        val barHeightRatio = (count.toFloat() / maxSteps).coerceIn(0.05f, 1f)
                        val isToday = date == LocalDate.now()
                        val dayLabel = date.dayOfWeek.name.take(3)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (count > 0) "${count / 1000}k" else "0",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height((130 * barHeightRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Days List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            weekly.reversed().forEach { (date, count) ->
                DayStepRow(date = date, count = count, goal = uiState.stepGoal, formatter = formatter)
            }
        }
    }
}

@Composable
private fun MonthlyStepsView(uiState: BiometricsState) {
    val monthly = uiState.monthlySteps
    val totalMonthly = monthly.sumOf { it.second }
    val averageMonthly = if (monthly.isNotEmpty()) totalMonthly / monthly.size else 0L
    val daysGoalReached = monthly.count { it.second >= uiState.stepGoal }
    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                title = stringResource(R.string.total_steps),
                value = String.format(Locale.getDefault(), "%,d", totalMonthly),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = stringResource(R.string.average_daily),
                value = String.format(Locale.getDefault(), "%,d", averageMonthly),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = stringResource(R.string.goal_reached_days),
                value = "$daysGoalReached",
                modifier = Modifier.weight(1f)
            )
        }

        // Monthly Overview Chart (30 Days Bars)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.monthly_steps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val maxSteps = (monthly.maxOfOrNull { it.second } ?: 10000L).coerceAtLeast(10000L)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthly.forEach { (_, count) ->
                        val barHeightRatio = (count.toFloat() / maxSteps).coerceIn(0.05f, 1f)
                        val isGoalMet = count >= uiState.stepGoal

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((140 * barHeightRatio).dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(
                                    if (isGoalMet) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }

        // Recent 10 Days List
        Text(
            text = "Histórico do Mês (Últimos dias)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            monthly.takeLast(10).reversed().forEach { (date, count) ->
                DayStepRow(date = date, count = count, goal = uiState.stepGoal, formatter = formatter)
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayStepRow(
    date: LocalDate,
    count: Long,
    goal: Int,
    formatter: DateTimeFormatter
) {
    val progress = (count.toFloat() / goal).coerceIn(0f, 1f)
    val isToday = date == LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isToday) "${date.format(formatter)} (${stringResource(R.string.today)})" else date.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (count >= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = String.format(Locale.getDefault(), "%,d", count),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
