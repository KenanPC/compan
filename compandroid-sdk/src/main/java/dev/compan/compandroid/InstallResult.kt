package dev.compan.compandroid

// Represents whether Android accepted the install intent handoff.
internal data class InstallResult(
    val started: Boolean,
    val message: String
)
