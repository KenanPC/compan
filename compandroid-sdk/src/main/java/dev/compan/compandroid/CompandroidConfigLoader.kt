package dev.compan.compandroid

import android.content.Context
import org.json.JSONObject

internal object CompandroidConfigLoader {
    fun load(context: Context): CompandroidConfig {
        val json = runCatching {
            context.assets.open("compan.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return CompandroidConfig.default(context.packageName)

        return runCatching {
            val root = JSONObject(json)
            val app = root.optJSONObject("app")
            val github = root.optJSONObject("github")
            val repoSlug = github?.optString("repository").orEmpty()
            val ownerRepo = repoSlug.split("/", limit = 2)

            CompandroidConfig(
                owner = github?.optString("owner").orEmpty().ifBlank { ownerRepo.getOrNull(0).orEmpty() },
                repo = github?.optString("repo").orEmpty().ifBlank { ownerRepo.getOrNull(1).orEmpty() },
                branch = github?.optString("branch").orEmpty().ifBlank { "compan-android" },
                workflowFileName = github?.optString("workflow").orEmpty()
                    .substringAfterLast("/")
                    .ifBlank { "compan-android-apk.yml" },
                artifactName = github?.optString("artifactName").orEmpty()
                    .ifBlank { "compan-android-debug-apk" },
                packageName = app?.optString("packageName").orEmpty().ifBlank { context.packageName }
            )
        }.getOrElse {
            CompandroidConfig.default(context.packageName)
        }
    }
}
