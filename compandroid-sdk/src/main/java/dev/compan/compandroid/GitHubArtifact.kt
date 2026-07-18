package dev.compan.compandroid

internal data class GitHubArtifact(
    val name: String,
    val downloadUrl: String,
    val workflowRunId: Long,
    val headSha: String
)

