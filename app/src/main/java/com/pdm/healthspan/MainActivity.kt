package com.pdm.healthspan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdm.healthspan.ui.home.Home
import com.pdm.healthspan.ui.theme.HealthspanTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel   = viewModel { MainViewModel() }
            HealthspanTheme {
                HealthspanApp(viewModel)
            }
        }
    }
}

enum class AppDestinations(
    // stringResource ID
    val stringResLabel: Int,
    // Pair<Int (inactive icon ID), Int (active icon ID)>
    val icons: Pair<Int, Int>
) {
    HOME(R.string.destination_home, R.drawable.ic_house to R.drawable.ic_house_fill),
    EXERCISES(R.string.destination_exercises, R.drawable.ic_clipboard_heart to R.drawable.ic_clipboard_heart_fill),
    SETTINGS(R.string.destination_settings, R.drawable.ic_gear to R.drawable.ic_gear_fill),
}

@Composable
fun HealthspanApp(viewModel: MainViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                val active = currentDestination == it

                item(
                    icon = {
                        Icon(
                            painter = if(!active) painterResource(it.icons.first)
                                      else        painterResource(it.icons.second),
                            contentDescription = stringResource(it.stringResLabel)
                        )
                    },
                    label = { Text(stringResource(it.stringResLabel)) },
                    selected = it == currentDestination,
                    onClick = { if(currentDestination != it) currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    // Assumes items are displayed on the same order as the enum
                    val direction = if (targetState.ordinal > initialState.ordinal) { 1 } else { -1 }
                    slideInHorizontally { it * direction } togetherWith slideOutHorizontally { it * -direction }
                }
            ) { current ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp)
                ) {
                    when(current) {
                        AppDestinations.HOME -> {
                            val uiState by viewModel.biometricsUiState.collectAsStateWithLifecycle()
                            Home(
                                uiState,
                                viewModel.desiredPermissions,
                                viewModel::onHomeEvent
                            )
                        }
                        AppDestinations.EXERCISES -> Text("exercises")
                        AppDestinations.SETTINGS -> Text("settings")
                    }
                }
            }
        }
    }
}