package com.pdm.healthspan.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.pdm.healthspan.R
import com.pdm.healthspan.ui.components.BiometricCharts
import com.pdm.healthspan.ui.states.BiometricsState
import java.util.Locale

@Composable
fun Home(
    uiState: BiometricsState,
    permissions: Set<String>,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToSteps: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if(uiState.validHealthConnectClient == null) {
            var client: HealthConnectClient? = null
            try { client = HealthConnectClient.getOrCreate(context) } catch (_: Exception) { }
            onEvent(HomeEvent.NewHealthConnectClient(client))
        }
    }

    // Health Connection ask for permissions launch activity
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ -> onEvent(HomeEvent.RefreshGrantedPermissions) }


    var showExerciseList by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = { onEvent(HomeEvent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Summary Card on Main Home Screen
            HomeStepCard(
                todaySteps = uiState.todaySteps,
                stepGoal = uiState.stepGoal,
                isSensorAvailable = uiState.isSensorAvailable,
                onNavigateToSteps = onNavigateToSteps
            )

            // Show exercise list or Biometric Charts
            if(showExerciseList) {
                ExerciseList(
                    data = uiState.exercices,
                    onCreate = { onEvent(HomeEvent.CreateExercise(it)) },
                    onDelete = { onEvent(HomeEvent.DeleteExercise(it)) },
                    closeTab = { showExerciseList = false }
                )
            } else {
                BiometricCharts(data = uiState)

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                TextButton(onClick = { showExerciseList = true }) { Text("Show exercise list") }

                // Permissions button
                Button(
                    enabled = uiState.grantedPermissions.size != permissions.size && uiState.validHealthConnectClient != null,
                    onClick = {
                        if(uiState.validHealthConnectClient == false) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
                            )

                            try { context.startActivity(intent) } catch (e: Exception) { println("Failed, ${e.message}") }
                        } else {
                            requestPermissionLauncher.launch(permissions)
                        }
                    }
                ) {
                    Text(text = if(uiState.validHealthConnectClient == false) stringResource(R.string.download_healthconnect)
                    else stringResource(R.string.grant_permissions))
                }
            }
        }
    }
}

@Composable
private fun HomeStepCard(
    todaySteps: Long,
    stepGoal: Int,
    isSensorAvailable: Boolean,
    onNavigateToSteps: () -> Unit
) {
    val progress = (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val distanceKm = String.format(Locale.getDefault(), "%.2f", todaySteps * 0.00075f)
    val caloriesKcal = (todaySteps * 0.04f).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.steps_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onNavigateToSteps) {
                    Text(text = stringResource(R.string.see_details))
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%,d", todaySteps),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "/ ${String.format(Locale.getDefault(), "%,d", stepGoal)} ${stringResource(R.string.steps).lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$distanceKm km • $caloriesKcal kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = if (isSensorAvailable) stringResource(R.string.sensor_active) else stringResource(R.string.sensor_inactive),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}