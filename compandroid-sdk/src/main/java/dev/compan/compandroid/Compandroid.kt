package dev.compan.compandroid

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.widget.CheckBox

object Compandroid {
    private var shakeDetector: ShakeDetector? = null
    private var launchNoticeShownThisProcess = false

    fun install(activity: Activity): Boolean {
        showLaunchNoticeIfNeeded(activity)
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

    private fun showLaunchNoticeIfNeeded(activity: Activity) {
        if (launchNoticeShownThisProcess || activity.isFinishing || activity.isDestroyed) return

        val prefs = CompandroidPrefs(activity)
        if (prefs.hideLaunchNotice) return

        launchNoticeShownThisProcess = true
        val hideFutureNotices = CheckBox(activity).apply {
            text = "Do not show this again"
            setPadding(32, 12, 32, 0)
        }

        AlertDialog.Builder(activity)
            .setTitle("Compandroid Enabled")
            .setMessage("You are using compandroid. To view extra app development settings shake your device.")
            .setView(hideFutureNotices)
            .setPositiveButton("OK") { _, _ ->
                if (hideFutureNotices.isChecked) {
                    prefs.hideLaunchNotice = true
                }
            }
            .show()
    }
}
