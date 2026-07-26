package dev.compan.compandroid

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.SensorManager
import android.os.Build
import android.provider.MediaStore
import android.widget.CheckBox
import java.io.File
import java.lang.ref.WeakReference

object Compandroid {
    private var shakeDetector: ShakeDetector? = null
    private var launchNoticeShownThisProcess = false
    private var hostActivity = WeakReference<Activity>(null)

    fun install(activity: Activity): Boolean {
        hostActivity = WeakReference(activity)
        showLaunchNoticeIfNeeded(activity)
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector?.stop()
        shakeDetector = ShakeDetector(sensorManager) {
            captureHostScreenToCache(activity)
            activity.startActivity(Intent(activity, CompandroidLandingActivity::class.java))
        }
        return shakeDetector?.start() == true
    }

    fun uninstall() {
        shakeDetector?.stop()
        shakeDetector = null
        hostActivity.clear()
    }

    internal fun hasPendingHostScreenshot(context: Context): Boolean =
        pendingScreenshotFile(context).isFile

    internal fun captureHostScreenshot(context: Context): Result<String> = runCatching {
        val pending = pendingScreenshotFile(context)
        require(pending.isFile) {
            "No screenshot is ready. Return to your app and shake the device again to capture the screen before CompanDROID opens."
        }

        val name = "compan-${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Compan")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create screenshot file.")

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                pending.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write screenshot file.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
            }
            pending.delete()
            name
        } catch (error: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    private fun captureHostScreenToCache(activity: Activity): Result<File> = runCatching {
        val root = activity.window.decorView.rootView
        require(root.width > 0 && root.height > 0) { "The app screen is not ready." }

        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        try {
            root.draw(Canvas(bitmap))
            val file = pendingScreenshotFile(activity)
            file.parentFile?.mkdirs()
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Could not prepare screenshot."
                }
            }
            file
        } finally {
            bitmap.recycle()
        }
    }

    private fun pendingScreenshotFile(context: Context): File =
        context.cacheDir.resolve("compandroid/pending-host-screen.png")

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
