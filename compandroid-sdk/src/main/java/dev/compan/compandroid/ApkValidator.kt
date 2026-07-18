package dev.compan.compandroid

import android.content.Context
import android.os.Build
import java.io.File

internal object ApkValidator {
    fun validateUpdate(context: Context, apk: File, expectedPackageName: String): ApkValidation {
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: return ApkValidation(false, "Downloaded APK could not be inspected")

        if (archive.packageName != expectedPackageName) {
            return ApkValidation(
                false,
                "APK package ${archive.packageName} does not match $expectedPackageName"
            )
        }

        val installed = packageManager.getPackageInfo(expectedPackageName, 0)
        val archiveVersion = archive.versionCodeCompat()
        val installedVersion = installed.versionCodeCompat()

        if (archiveVersion <= installedVersion) {
            return ApkValidation(
                false,
                "APK versionCode $archiveVersion is not newer than installed $installedVersion"
            )
        }

        return ApkValidation(true, "APK validated: versionCode $archiveVersion")
    }

    private fun android.content.pm.PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()
}

