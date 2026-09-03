package dev.compan.compandroid

// Carries both the user-facing validation result and version details for follow-up UI.
internal data class ApkValidation(
    val ok: Boolean,
    val message: String,
    val archiveVersionCode: Long? = null,
    val installedVersionCode: Long? = null
)
