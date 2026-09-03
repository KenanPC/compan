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
        // Defaults match the generated Android workflow and artifact names.
        fun default(packageName: String = ""): CompandroidConfig = CompandroidConfig(
            owner = "",
            repo = "",
            branch = "compan-android",
            workflowFileName = "compan-android-apk.yml",
            artifactName = "compan-android-debug-apk",
            packageName = packageName
        )
    }
}
