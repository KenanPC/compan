package dev.compan.compandroid

data class CompandroidConfig(
    val owner: String,
    val repo: String,
    val branch: String,
    val workflowFileName: String,
    val artifactName: String,
    val packageName: String
) {
    companion object {
        fun default(): CompandroidConfig = CompandroidConfig(
            owner = "",
            repo = "",
            branch = "compan-android",
            workflowFileName = "compan-android-apk.yml",
            artifactName = "compan-android-debug-apk",
            packageName = ""
        )
    }
}

