package dev.compan.compandroid

internal data class GitHubWorkflow(
    val name: String,
    val path: String,
    val state: String
) {
    // UI matching uses the file name even when GitHub returns a full workflow path.
    val fileName: String = path.substringAfterLast("/")
}
