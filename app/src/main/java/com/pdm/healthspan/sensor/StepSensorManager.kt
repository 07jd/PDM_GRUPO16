package com.pdm.healthspan.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val prefs = context.getSharedPreferences("healthspan_step_prefs", Context.MODE_PRIVATE)

    private val _liveStepsToday = MutableStateFlow(0L)
    val liveStepsToday = _liveStepsToday.asStateFlow()

    val isSensorAvailable: Boolean
        get() = stepCounterSensor != null || stepDetectorSensor != null

    private var initialCounterBaseline = -1f

    fun startListening() {
        val todayStr = LocalDate.now().toString()
        val savedDate = prefs.getString("last_date", "")

        if (savedDate != todayStr) {
            prefs.edit()
                .putString("last_date", todayStr)
                .putLong("saved_steps_today", 0L)
                .putFloat("counter_baseline", -1f)
                .apply()
        }

        val savedSteps = prefs.getLong("saved_steps_today", 0L)
        _liveStepsToday.value = savedSteps
        initialCounterBaseline = prefs.getFloat("counter_baseline", -1f)

        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSinceReboot = event.values[0]
                if (initialCounterBaseline < 0f) {
                    initialCounterBaseline = totalSinceReboot - _liveStepsToday.value
                    prefs.edit().putFloat("counter_baseline", initialCounterBaseline).apply()
                }
                val currentSteps = (totalSinceReboot - initialCounterBaseline).coerceAtLeast(0f).toLong()
                _liveStepsToday.value = currentSteps
                prefs.edit().putLong("saved_steps_today", currentSteps).apply()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    val updated = _liveStepsToday.value + 1
                    _liveStepsToday.value = updated
                    prefs.edit().putLong("saved_steps_today", updated).apply()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
