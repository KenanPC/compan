package dev.compan.compandroid

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

internal class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit
) : SensorEventListener {
    private var lastShakeAt = 0L

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val force = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (force > 27f && now - lastShakeAt > 1500L) {
            lastShakeAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

