package com.pdm.healthspan.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.pdm.healthspan.R
import com.pdm.healthspan.ui.components.BiometricCharts
import com.pdm.healthspan.ui.states.BiometricsState
import androidx.core.net.toUri

@Composable
fun Home(
    uiState: BiometricsState,
    permissions: Set<String>,
    onEvent: (HomeEvent) -> Unit
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


    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = { onEvent(HomeEvent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BiometricCharts(data = uiState)

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

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