package dev.compan.compandroid

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.common.api.ApiException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.concurrent.thread

class CompandroidSettingsActivity : Activity() {
    private lateinit var prefs: CompandroidPrefs
    private lateinit var status: TextView
    private lateinit var owner: TitledField
    private lateinit var repo: TitledField
    private lateinit var branch: TitledField
    private lateinit var workflow: TitledField
    private lateinit var artifact: TitledField
    private lateinit var apkVersion: TitledField
    private lateinit var token: TitledField
    private lateinit var selectedRepo: TextView
    private lateinit var repositorySection: LinearLayout
    private lateinit var chooseRepo: Button
    private lateinit var githubHeader: LinearLayout
    private lateinit var githubHeaderTitle: TextView
    private lateinit var githubHeaderMeta: TextView
    private lateinit var githubHeaderCaret: TextView
    private lateinit var githubSettingsSection: LinearLayout
    private lateinit var githubDetails: LinearLayout
    private lateinit var test: Button
    private lateinit var pull: Button
    private var detailsExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CompandroidPrefs(this)
        title = "Compandroid"

        val hasToken = prefs.token.isNotBlank()
        token = field("Paste read-only token", prefs.token, hidden = true, cameraButton = true)
        owner = field("Owner", if (hasToken) prefs.owner else "")
        repo = field("Repository", if (hasToken) prefs.repo else "")
        branch = field("Branch", if (hasToken) prefs.branch else "")
        workflow = field("Workflow file", if (hasToken) prefs.workflowFileName else "")
        artifact = field("APK artifact name", if (hasToken) prefs.artifactName else "")
        apkVersion = field("Installed APK version", installedApkVersionLabel())
        apkVersion.editText.isEnabled = false

        status = TextView(this).apply {
            text = if (hasToken) "Token ready. Choose the repository that publishes your APK." else ""
            textSize = 14f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, dp(16), 0, dp(16))
        }

        selectedRepo = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, 0, 0, dp(12))
        }

        chooseRepo = Button(this).apply {
            text = "Choose repository"
            stylePrimaryButton()
            setOnClickListener {
                saveFields()
                if (prefs.token.isBlank()) {
                    status.text = "Paste or scan a read-only GitHub token first."
                } else {
                    status.text = "Loading repositories..."
                    loadRepositories(prefs.token)
                }
            }
        }

        repositorySection = panel().apply {
            addView(eyebrow("Step 2"))
            addView(title("Choose repository", 18f))
            addView(body("Pick the GitHub repo that contains the Compandroid APK build workflow."))
            addView(selectedRepo)
            addView(chooseRepo, matchWrapParams(top = dp(4)))
        }

        githubHeaderTitle = TextView(this).apply {
            text = "GitHub settings"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(15, 23, 42))
        }
        githubHeaderMeta = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            setPadding(0, dp(4), 0, 0)
        }
        githubHeaderCaret = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.rgb(51, 65, 85))
            setPadding(dp(16), 0, 0, 0)
        }
        githubHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isClickable = true
            isFocusable = true
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                detailsExpanded = !detailsExpanded
                updateGitHubSection()
            }
            addView(LinearLayout(this@CompandroidSettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(githubHeaderTitle)
                addView(githubHeaderMeta)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(githubHeaderCaret)
        }

        githubDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, 0)
            addView(owner.container)
            addView(repo.container)
            addView(branch.container)
            addView(workflow.container)
            addView(artifact.container)
            addView(apkVersion.container)
        }

        val save = Button(this).apply {
            text = "Save settings"
            styleSecondaryButton()
            setOnClickListener {
                val hadNoToken = prefs.token.isBlank()
                saveFields()
                updateGitHubSection()
                if (hadNoToken && prefs.token.isNotBlank() && repo.editText.text.isBlank()) {
                    status.text = "Token saved. Loading repositories..."
                    loadRepositories(prefs.token)
                } else {
                    status.text = "Settings saved"
                }
            }
        }

        token.actionButton?.setOnClickListener {
            saveFields()
            scanTokenQr()
        }

        pull = Button(this).apply {
            text = "Pull latest APK"
            stylePrimaryButton()
            setOnClickListener {
                save.performClick()
                pullLatest()
            }
        }

        test = Button(this).apply {
            text = "Test GitHub connection"
            styleSecondaryButton()
            setOnClickListener {
                save.performClick()
                testConnection()
            }
        }

        val clearToken = Button(this).apply {
            text = "Clear token"
            styleTextButton()
            setOnClickListener {
                prefs.clearToken()
                prefs.owner = ""
                prefs.repo = ""
                prefs.branch = ""
                prefs.workflowFileName = ""
                prefs.artifactName = ""
                token.editText.setText("")
                clearConfigFields()
                detailsExpanded = false
                updateGitHubSection()
                status.text = "Token cleared"
            }
        }

        githubSettingsSection = panel().apply {
            addView(githubHeader)
            addView(githubDetails)
            addView(save, matchWrapParams(top = dp(12)))
            addView(test, matchWrapParams(top = dp(8)))
            addView(clearToken, matchWrapParams(top = dp(8)))
        }

        token.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s?.toString()?.trim().orEmpty()
                if (value.isBlank()) {
                    prefs.clearToken()
                } else if (isLikelyGitHubToken(value)) {
                    prefs.token = parseGitHubToken(value).ifBlank { value }
                }
                updateGitHubSection()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
            setPadding(dp(24), statusBarHeight() + dp(24), dp(24), dp(32))
            addView(heading())
            addView(description())
            addView(tokenPanel())
            addView(repositorySection, matchWrapParams(top = dp(16)))
            addView(githubSettingsSection, matchWrapParams(top = dp(16)))
            addView(pull, matchWrapParams(top = dp(16)))
            addView(status)
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.rgb(248, 250, 252))
            addView(layout, ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })
        updateGitHubSection()
    }

    override fun onResume() {
        super.onResume()
        if (::apkVersion.isInitialized) {
            apkVersion.editText.setText(installedApkVersionLabel())
        }
    }

    private fun saveFields() {
        prefs.owner = owner.editText.text.toString().trim()
        prefs.repo = repo.editText.text.toString().trim()
        prefs.branch = branch.editText.text.toString().trim()
        prefs.workflowFileName = workflow.editText.text.toString().trim()
        prefs.artifactName = artifact.editText.text.toString().trim()
        prefs.token = token.editText.text.toString().trim()
    }

    private fun updateGitHubSection() {
        val tokenText = token.editText.text.toString().trim()
        val hasToken = tokenText.isNotBlank() && (isLikelyGitHubToken(tokenText) || prefs.token.isNotBlank())
        val hasRepo = repo.editText.text.toString().trim().isNotBlank()
        apkVersion.editText.setText(installedApkVersionLabel())

        repositorySection.visibility = if (hasToken) View.VISIBLE else View.GONE
        chooseRepo.text = if (hasRepo) "Change repository" else "Choose repository"

        val fullName = "${owner.editText.text}/${repo.editText.text}"
        selectedRepo.visibility = if (hasRepo) View.VISIBLE else View.GONE
        selectedRepo.text = if (hasRepo) "Selected: $fullName" else ""

        githubSettingsSection.visibility = if (hasToken && hasRepo) View.VISIBLE else View.GONE
        githubDetails.visibility = if (hasToken && hasRepo && detailsExpanded) View.VISIBLE else View.GONE
        githubHeaderCaret.text = if (detailsExpanded) "^" else "v"
        githubHeaderMeta.text = if (hasRepo) fullName else "Select a repository first"
        listOf(owner, repo, branch, workflow, artifact).forEach { field ->
            field.editText.isEnabled = hasToken
        }
        apkVersion.editText.isEnabled = false
        test.isEnabled = hasToken && hasRepo
        pull.isEnabled = hasToken && hasRepo
        pull.visibility = if (hasToken && hasRepo) View.VISIBLE else View.GONE
    }

    private fun clearConfigFields() {
        listOf(owner, repo, branch, workflow, artifact).forEach { field -> field.editText.setText("") }
    }

    private fun scanTokenQr() {
        status.text = "Opening QR scanner..."
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(this, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val scannedToken = parseGitHubToken(barcode.rawValue.orEmpty())
                if (scannedToken.isBlank()) {
                    status.text = "QR code did not contain a GitHub token."
                    return@addOnSuccessListener
                }
                prefs.token = scannedToken
                token.editText.setText(scannedToken)
                updateGitHubSection()
                status.text = "Token scanned. Loading repositories..."
                loadRepositories(scannedToken)
            }
            .addOnCanceledListener {
                status.text = "QR scan cancelled"
            }
            .addOnFailureListener { error ->
                status.text = when (error) {
                    is ApiException -> "QR scanner unavailable (${error.statusCode}). Paste token below."
                    else -> error.message ?: "QR scanner unavailable. Paste token below."
                }
            }
    }

    private fun parseGitHubToken(rawValue: String): String {
        val trimmed = rawValue.trim()
        if (
            trimmed.startsWith("ghp_") ||
            trimmed.startsWith("github_pat_") ||
            trimmed.startsWith("gho_") ||
            trimmed.startsWith("ghu_")
        ) {
            return trimmed
        }

        trimmed.lineSequence().forEach { line ->
            val cleanLine = line.trim().removePrefix("export ").trim()
            val separator = cleanLine.indexOf("=")
            if (separator > 0) {
                val key = cleanLine.substring(0, separator).trim()
                if (key == "COMPANDROID_GITHUB_TOKEN" || key == "GITHUB_TOKEN") {
                    return cleanLine.substring(separator + 1).trim().trim('"', '\'')
                }
            }
        }

        return ""
    }

    private fun isLikelyGitHubToken(value: String): Boolean =
        parseGitHubToken(value).isNotBlank() || value.length >= 20

    private fun installedApkVersionLabel(): String {
        val targetPackage = prefs.config(packageName).packageName.ifBlank { packageName }
        return runCatching {
            val packageInfo = packageManager.getPackageInfo(targetPackage, 0)
            val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
            "$versionName (versionCode $versionCode)"
        }.getOrElse {
            "Not installed"
        }
    }

    private fun loadRepositories(token: String) {
        thread {
            runCatching {
                GitHubActionsClient(token).repositories()
            }.onSuccess { repositories ->
                runOnUiThread { showRepositoryPicker(repositories) }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Could not load repositories" }
            }
        }
    }

    private fun showRepositoryPicker(repositories: List<GitHubRepository>) {
        if (repositories.isEmpty()) {
            status.text = "GitHub access works, but no accessible repositories were found."
            return
        }

        val labels = repositories.map { repository ->
            repository.fullName + if (repository.private) "  private" else ""
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Repository")
            .setItems(labels) { _, which ->
                val selected = repositories[which]
                owner.editText.setText(selected.owner)
                repo.editText.setText(selected.name)
                prefs.owner = selected.owner
                prefs.repo = selected.name
                if (branch.editText.text.isBlank()) {
                    branch.editText.setText("compan-android")
                    prefs.branch = "compan-android"
                }
                if (artifact.editText.text.isBlank()) {
                    artifact.editText.setText("compan-android-debug-apk")
                    prefs.artifactName = "compan-android-debug-apk"
                }
                detailsExpanded = false
                updateGitHubSection()
                status.text = "Selected ${selected.fullName}. Loading workflows..."
                loadWorkflows(selected)
            }
            .show()
    }

    private fun loadWorkflows(repository: GitHubRepository) {
        thread {
            runCatching {
                GitHubActionsClient(prefs.token).workflows(repository.owner, repository.name)
                    .filter { it.state == "active" }
            }.onSuccess { workflows ->
                runOnUiThread { showWorkflowPicker(workflows) }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Could not load workflows" }
            }
        }
    }

    private fun showWorkflowPicker(workflows: List<GitHubWorkflow>) {
        if (workflows.isEmpty()) {
            status.text = "Repository selected, but no active workflows were found."
            return
        }

        val labels = workflows.map { "${it.name}  ${it.fileName}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Workflow")
            .setItems(labels) { _, which ->
                val selected = workflows[which]
                workflow.editText.setText(selected.fileName)
                prefs.workflowFileName = selected.fileName
                updateGitHubSection()
                status.text = "Selected ${selected.fileName}. GitHub settings are available below."
            }
            .show()
    }

    private fun pullLatest() {
        status.text = "Checking GitHub Actions..."
        thread {
            runCatching {
                val config = prefs.config(packageName)
                require(config.owner.isNotBlank() && config.repo.isNotBlank()) {
                    "Select a GitHub repository first"
                }
                val client = GitHubActionsClient(prefs.token)
                val latestArtifact = client.latestSuccessfulArtifact(config)
                    ?: error("No matching successful APK artifact found")
                val installedVersionCode = ApkValidator.installedVersionCode(this, config.packageName)
                if (prefs.wasArtifactCheckedAsNotNewer(config, latestArtifact, installedVersionCode)) {
                    error(
                        "Latest GitHub APK was already checked and is not newer than installed " +
                            "versionCode $installedVersionCode. Push a build with a higher versionCode."
                    )
                }
                val apk = client.downloadArtifactApk(latestArtifact, cacheDir.resolve("compandroid"))
                val validation = ApkValidator.validateUpdate(this, apk, config.packageName)
                if (
                    !validation.ok &&
                    validation.archiveVersionCode != null &&
                    validation.installedVersionCode != null &&
                    validation.archiveVersionCode <= validation.installedVersionCode
                ) {
                    prefs.rememberArtifactNotNewer(config, latestArtifact, validation.installedVersionCode)
                }
                require(validation.ok) { validation.message }
                prefs.clearArtifactNotNewer()
                runOnUiThread {
                    val install = ApkInstaller.install(this, apk)
                    status.text = "${validation.message}. ${install.message} ${latestArtifact.headSha.take(7)}"
                }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Pull failed" }
            }
        }
    }

    private fun testConnection() {
        status.text = "Testing GitHub access..."
        thread {
            runCatching {
                val config = prefs.config(packageName)
                require(config.owner.isNotBlank() && config.repo.isNotBlank()) {
                    "Select a GitHub repository first"
                }
                val latestArtifact = GitHubActionsClient(prefs.token).latestSuccessfulArtifact(config)
                runOnUiThread {
                    status.text = if (latestArtifact == null) {
                        "GitHub access works, but no matching APK artifact was found."
                    } else {
                        "GitHub access works. Latest artifact: ${latestArtifact.name} from ${latestArtifact.headSha.take(7)}"
                    }
                }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Connection test failed" }
            }
        }
    }

    private fun heading(): TextView = TextView(this).apply {
        text = "Compandroid"
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 23, 42))
        setPadding(0, 0, 0, dp(6))
    }

    private fun description(): TextView = TextView(this).apply {
        text = "First, get a GitHub read-only token. Ask your LLM to guide you through creating a fine-grained token with read-only access to the repository you want Compandroid to pull builds from."
        textSize = 14f
        setTextColor(Color.rgb(71, 85, 105))
        setLineSpacing(0f, 1.15f)
        setPadding(0, 0, 0, dp(20))
    }

    private fun tokenPanel(): LinearLayout = panel().apply {
        addView(eyebrow("Step 1"))
        addView(title("Add GitHub read token", 18f))
        addView(body("Paste the token here or scan the QR code generated on your desktop."))
        addView(token.container, matchWrapParams(top = dp(10)))
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(Color.WHITE, Color.rgb(226, 232, 240), 1f)
    }

    private fun eyebrow(textValue: String): TextView = TextView(this).apply {
        text = textValue.uppercase()
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 118, 110))
        setPadding(0, 0, 0, dp(6))
    }

    private fun title(textValue: String, size: Float): TextView = TextView(this).apply {
        text = textValue
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 23, 42))
    }

    private fun body(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 14f
        setTextColor(Color.rgb(71, 85, 105))
        setLineSpacing(0f, 1.12f)
        setPadding(0, dp(6), 0, dp(8))
    }

    private fun field(
        label: String,
        value: String,
        hidden: Boolean = false,
        cameraButton: Boolean = false
    ): TitledField {
        val title = TextView(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(51, 65, 85))
            setPadding(0, dp(10), 0, dp(4))
        }
        val editText = EditText(this).apply {
            hint = label
            setText(value)
            setSingleLine(true)
            textSize = 15f
            setPadding(dp(12), 0, dp(12), 0)
            minHeight = dp(48)
            background = rounded(Color.WHITE, Color.rgb(203, 213, 225), 1f)
            inputType = if (hidden) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT
            }
        }
        val actionButton = if (cameraButton) {
            ImageButton(this).apply {
                contentDescription = "Scan token QR"
                setImageResource(android.R.drawable.ic_menu_camera)
                setBackgroundColor(0x00000000)
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }
        } else {
            null
        }
        val fieldRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(editText, LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ))
            actionButton?.let {
                addView(it, LinearLayout.LayoutParams(
                    dp(52),
                    dp(52)
                ).apply { leftMargin = dp(8) })
            }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(fieldRow)
        }
        return TitledField(label, container, editText, actionButton)
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun Button.stylePrimaryButton() {
        setAllCaps(false)
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        minHeight = dp(48)
        background = rounded(Color.rgb(15, 118, 110), Color.TRANSPARENT, 0f)
    }

    private fun Button.styleSecondaryButton() {
        setAllCaps(false)
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 23, 42))
        minHeight = dp(48)
        background = rounded(Color.rgb(241, 245, 249), Color.rgb(203, 213, 225), 1f)
    }

    private fun Button.styleTextButton() {
        setAllCaps(false)
        textSize = 15f
        setTextColor(Color.rgb(185, 28, 28))
        minHeight = dp(44)
        background = rounded(Color.TRANSPARENT, Color.TRANSPARENT, 0f)
    }

    private fun rounded(fill: Int, stroke: Int, strokeWidthDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(fill)
            if (strokeWidthDp > 0f) {
                setStroke(dp(strokeWidthDp), stroke)
            }
        }

    private fun matchWrapParams(top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = top }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private data class TitledField(
        val label: String,
        val container: LinearLayout,
        val editText: EditText,
        val actionButton: ImageButton? = null
    )
}
