package dev.compan.compandroid

internal data class GitHubWorkflow(
    val name: String,
    val path: String,
    val state: String
) {
    val fileName: String = path.substringAfterLast("/")
}
