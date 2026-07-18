package dev.compan.compandroid

import android.content.Context

internal class CompandroidPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("compandroid", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit().putString("token", value).apply()

    var owner: String
        get() = prefs.getString("owner", "") ?: ""
        set(value) = prefs.edit().putString("owner", value).apply()

    var repo: String
        get() = prefs.getString("repo", "") ?: ""
        set(value) = prefs.edit().putString("repo", value).apply()

    var branch: String
        get() = prefs.getString("branch", "compan-android") ?: "compan-android"
        set(value) = prefs.edit().putString("branch", value).apply()

    var artifactName: String
        get() = prefs.getString("artifactName", "compan-android-debug-apk") ?: "compan-android-debug-apk"
        set(value) = prefs.edit().putString("artifactName", value).apply()

    fun config(packageName: String): CompandroidConfig = CompandroidConfig(
        owner = owner,
        repo = repo,
        branch = branch,
        workflowFileName = "compan-android-apk.yml",
        artifactName = artifactName,
        packageName = packageName
    )
}

