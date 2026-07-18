package dev.compan.compandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

internal object ApkInstaller {
    fun install(context: Context, apk: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return InstallResult(
                false,
                "Allow installs from this app, then return to Compandroid and pull again."
            )
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.compandroid.files",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (intent.resolveActivity(context.packageManager) == null) {
            return InstallResult(false, "No Android package installer is available on this device.")
        }

        context.startActivity(intent)
        return InstallResult(true, "Android installer opened.")
    }
}
