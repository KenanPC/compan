package dev.compan.compandroid

// Keeps GitHub API failures distinct from local validation and install errors.
internal class GitHubApiException(message: String) : RuntimeException(message)
