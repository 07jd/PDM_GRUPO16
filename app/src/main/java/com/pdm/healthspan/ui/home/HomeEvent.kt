package com.pdm.healthspan.ui.home

import androidx.health.connect.client.HealthConnectClient

sealed interface HomeEvent {
    class NewHealthConnectClient(val client: HealthConnectClient?)  : HomeEvent
    object Refresh                                                  : HomeEvent
    object RefreshGrantedPermissions                                : HomeEvent
}