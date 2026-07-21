package dev.compan.compandroid

internal data class ApkValidation(
    val ok: Boolean,
    val message: String,
    val archiveVersionCode: Long? = null,
    val installedVersionCode: Long? = null
)
