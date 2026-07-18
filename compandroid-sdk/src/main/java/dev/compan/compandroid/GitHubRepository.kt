package dev.compan.compandroid

internal data class GitHubRepository(
    val fullName: String,
    val owner: String,
    val name: String,
    val private: Boolean
)
