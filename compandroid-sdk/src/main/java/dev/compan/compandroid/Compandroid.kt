package dev.compan.compandroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager

object Compandroid {
    private var shakeDetector: ShakeDetector? = null

    fun install(activity: Activity): Boolean {
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector?.stop()
        shakeDetector = ShakeDetector(sensorManager) {
            activity.startActivity(Intent(activity, CompandroidSettingsActivity::class.java))
        }
        return shakeDetector?.start() == true
    }

    fun uninstall() {
        shakeDetector?.stop()
        shakeDetector = null
    }
}
