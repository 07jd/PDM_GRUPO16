package com.pdm.healthspan.ui.home

import androidx.health.connect.client.HealthConnectClient
import kotlin.time.Instant

sealed interface HomeEvent {
    class NewHealthConnectClient(val client: HealthConnectClient?)  : HomeEvent
    class CreateExercise(val data: Triple<Instant,Instant, Int> )   : HomeEvent
    class DeleteExercise(val id: String)                            : HomeEvent
    object Refresh                                                  : HomeEvent
    object RefreshGrantedPermissions                                : HomeEvent
}