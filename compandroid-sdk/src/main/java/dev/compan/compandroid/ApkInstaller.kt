package dev.compan.compandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal object ApkInstaller {
    fun install(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.compandroid.files",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }
}

