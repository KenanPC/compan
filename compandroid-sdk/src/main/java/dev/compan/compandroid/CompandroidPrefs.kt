package dev.compan.compandroid

import android.content.Context

internal class CompandroidPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("compandroid", Context.MODE_PRIVATE)
    private val bundledConfig = CompandroidConfigLoader.load(context)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit().putString("token", value).apply()

    var owner: String
        get() = prefs.getString("owner", null) ?: bundledConfig.owner
        set(value) = prefs.edit().putString("owner", value).apply()

    var repo: String
        get() = prefs.getString("repo", null) ?: bundledConfig.repo
        set(value) = prefs.edit().putString("repo", value).apply()

    var branch: String
        get() = prefs.getString("branch", null) ?: bundledConfig.branch
        set(value) = prefs.edit().putString("branch", value).apply()

    var artifactName: String
        get() = prefs.getString("artifactName", null) ?: bundledConfig.artifactName
        set(value) = prefs.edit().putString("artifactName", value).apply()

    var workflowFileName: String
        get() = prefs.getString("workflowFileName", null) ?: bundledConfig.workflowFileName
        set(value) = prefs.edit().putString("workflowFileName", value).apply()

    var hideLaunchNotice: Boolean
        get() = prefs.getBoolean("hideLaunchNotice", false)
        set(value) = prefs.edit().putBoolean("hideLaunchNotice", value).apply()

    fun wasArtifactCheckedAsNotNewer(
        config: CompandroidConfig,
        artifact: GitHubArtifact,
        installedVersionCode: Long
    ): Boolean =
        prefs.getString("notNewerOwner", "") == config.owner &&
            prefs.getString("notNewerRepo", "") == config.repo &&
            prefs.getString("notNewerBranch", "") == config.branch &&
            prefs.getString("notNewerWorkflow", "") == config.workflowFileName &&
            prefs.getString("notNewerArtifactName", "") == config.artifactName &&
            prefs.getLong("notNewerWorkflowRunId", -1L) == artifact.workflowRunId &&
            prefs.getString("notNewerHeadSha", "") == artifact.headSha &&
            prefs.getLong("notNewerInstalledVersionCode", -1L) == installedVersionCode

    fun rememberArtifactNotNewer(
        config: CompandroidConfig,
        artifact: GitHubArtifact,
        installedVersionCode: Long
    ) {
        prefs.edit()
            .putString("notNewerOwner", config.owner)
            .putString("notNewerRepo", config.repo)
            .putString("notNewerBranch", config.branch)
            .putString("notNewerWorkflow", config.workflowFileName)
            .putString("notNewerArtifactName", config.artifactName)
            .putLong("notNewerWorkflowRunId", artifact.workflowRunId)
            .putString("notNewerHeadSha", artifact.headSha)
            .putLong("notNewerInstalledVersionCode", installedVersionCode)
            .apply()
    }

    fun clearArtifactNotNewer() {
        prefs.edit()
            .remove("notNewerOwner")
            .remove("notNewerRepo")
            .remove("notNewerBranch")
            .remove("notNewerWorkflow")
            .remove("notNewerArtifactName")
            .remove("notNewerWorkflowRunId")
            .remove("notNewerHeadSha")
            .remove("notNewerInstalledVersionCode")
            .apply()
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
    }

    fun config(packageName: String): CompandroidConfig = CompandroidConfig(
        owner = owner,
        repo = repo,
        branch = branch,
        workflowFileName = workflowFileName,
        artifactName = artifactName,
        packageName = bundledConfig.packageName.ifBlank { packageName }
    )
}
