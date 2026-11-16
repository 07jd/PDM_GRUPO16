package com.pdm.healthspan.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.pdm.healthspan.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

@Composable
fun ExerciseList(
    data: List<ExerciseSessionRecord>?,
    onCreate: (Triple<Instant,Instant,Int>) -> Unit,
    onDelete: (String) -> Unit,
    closeTab: () -> Unit
) {
    BackHandler(true) { closeTab() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(start = 15.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { closeTab() }) { Text(stringResource(R.string.back))}
            }
        },
        floatingActionButton = {
            TextButton(onClick = { showAddDialog = true }) { Text("Add") }
        }
    ) { innerPadding ->
        if(showAddDialog) {
            AddExerciseDialog(
                onDismiss = { showAddDialog = false },
                onSubmit  = { onCreate(it); showAddDialog = false }
            )
        }

        if(data == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(R.string.no_permission)) }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top)
        ) {
            items(data) {
                ExerciseCard(it) { id -> onDelete(id) }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseSessionRecord,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when(exercise.exerciseType) {
                    56 -> "Running"
                    79 -> "Walking"
                    else -> "Other (${exercise.exerciseType})"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${exercise.startTime} - ${exercise.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = { onDelete(exercise.metadata.id) }) { Text("X") }
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onSubmit: (Triple<Instant, Instant, Int>) -> Unit
) {
    val types = listOf(
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    )

    var selected by remember { mutableIntStateOf(0) }

    var startTime by remember { mutableStateOf(LocalTime.now().minusHours(1)) }
    var endTime by remember { mutableStateOf(LocalTime.now()) }

    @OptIn(ExperimentalMaterial3Api::class)
    val startPicker = rememberTimePickerState(
        initialHour = startTime.hour,
        initialMinute = startTime.minute,
        is24Hour = true
    )

    @OptIn(ExperimentalMaterial3Api::class)
    val endPicker = rememberTimePickerState(
        initialHour = endTime.hour,
        initialMinute = endTime.minute,
        is24Hour = true
    )

    @OptIn(ExperimentalMaterial3Api::class)
    BasicAlertDialog(
        onDismissRequest = { onDismiss() }
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Start time", style = MaterialTheme.typography.titleMedium)
                TimeInput(state = startPicker)

                Text("End time", style = MaterialTheme.typography.titleMedium)
                TimeInput(state = endPicker)


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEachIndexed { index, type ->
                        Button(
                            onClick = { selected = index },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected == index)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected == index)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(if (type == ExerciseSessionRecord.EXERCISE_TYPE_WALKING) "Walking" else "Running")
                        }
                    }
                }

                val startInstant = LocalTime.of(startPicker.hour, startPicker.minute)
                    .atDate(LocalDate.now())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toKotlinInstant()

                val endInstant = LocalTime.of(endPicker.hour, endPicker.minute)
                    .atDate(LocalDate.now())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toKotlinInstant()

                Button(
                    enabled = endInstant <= Clock.System.now() && endInstant > startInstant,
                    onClick = { onSubmit(
                        Triple(
                            startInstant,
                            endInstant,
                            types[selected]
                    ))}
                ) {
                    Text("Create")
                }
            }
        }
    }
}