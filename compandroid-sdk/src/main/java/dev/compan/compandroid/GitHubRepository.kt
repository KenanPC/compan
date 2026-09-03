package dev.compan.compandroid

// Minimal repository metadata needed for selection and private-repo hints.
internal data class GitHubRepository(
    val fullName: String,
    val owner: String,
    val name: String,
    val private: Boolean
)
