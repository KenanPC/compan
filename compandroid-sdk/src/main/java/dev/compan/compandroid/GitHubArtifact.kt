package dev.compan.compandroid

// Preserve workflow run identity so repeated "not newer" checks can be skipped.
internal data class GitHubArtifact(
    val name: String,
    val downloadUrl: String,
    val workflowRunId: Long,
    val headSha: String
)
