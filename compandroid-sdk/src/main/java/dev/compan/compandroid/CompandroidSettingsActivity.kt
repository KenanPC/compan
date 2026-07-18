package dev.compan.compandroid

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
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
    private lateinit var token: TitledField
    private lateinit var repoSummary: Button
    private lateinit var chooseRepo: Button
    private lateinit var githubDetails: LinearLayout
    private lateinit var test: Button
    private lateinit var pull: Button
    private var detailsExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CompandroidPrefs(this)
        title = "Compandroid"

        val hasToken = prefs.token.isNotBlank()
        token = field("GitHub read token", prefs.token, hidden = true, cameraButton = true)
        owner = field("Owner", if (hasToken) prefs.owner else "")
        repo = field("Repository", if (hasToken) prefs.repo else "")
        branch = field("Branch", if (hasToken) prefs.branch else "")
        workflow = field("Workflow file", if (hasToken) prefs.workflowFileName else "")
        artifact = field("APK artifact name", if (hasToken) prefs.artifactName else "")

        status = TextView(this).apply {
            text = if (hasToken) "Ready" else "Scan or paste a read-only GitHub token to begin."
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }

        repoSummary = Button(this).apply {
            setAllCaps(false)
            setOnClickListener {
                detailsExpanded = !detailsExpanded
                updateGitHubSection()
            }
        }

        chooseRepo = Button(this).apply {
            text = "Choose Repository"
            setOnClickListener {
                saveFields()
                if (prefs.token.isBlank()) {
                    status.text = "Scan or paste a read-only GitHub token first."
                } else {
                    status.text = "Loading repositories..."
                    loadRepositories(prefs.token)
                }
            }
        }

        githubDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(owner.container)
            addView(branch.container)
            addView(workflow.container)
            addView(artifact.container)
        }

        val save = Button(this).apply {
            text = "Save Settings"
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
            text = "Pull Latest APK"
            setOnClickListener {
                save.performClick()
                pullLatest()
            }
        }

        test = Button(this).apply {
            text = "Test GitHub Connection"
            setOnClickListener {
                save.performClick()
                testConnection()
            }
        }

        val clearToken = Button(this).apply {
            text = "Clear Token"
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, statusBarHeight() + 24, 32, 32)
            addView(heading())
            addView(description())
            addView(sectionTitle("GitHub"))
            addView(token.container)
            addView(repoSummary)
            addView(chooseRepo)
            addView(githubDetails)
            addView(save)
            addView(test)
            addView(pull)
            addView(clearToken)
            addView(status)
        }

        setContentView(ScrollView(this).apply { addView(layout) })
        updateGitHubSection()
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
        val hasToken = token.editText.text.toString().trim().isNotBlank() || prefs.token.isNotBlank()
        val hasRepo = repo.editText.text.toString().trim().isNotBlank()

        chooseRepo.visibility = if (hasToken) View.VISIBLE else View.GONE
        chooseRepo.text = if (hasRepo) "Change Repository" else "Choose Repository"

        repoSummary.visibility = if (hasToken && hasRepo) View.VISIBLE else View.GONE
        repoSummary.text = if (detailsExpanded) {
            "v ${owner.editText.text}/${repo.editText.text}"
        } else {
            "> ${owner.editText.text}/${repo.editText.text}"
        }

        githubDetails.visibility = if (hasToken && hasRepo && detailsExpanded) View.VISIBLE else View.GONE
        listOf(owner, branch, workflow, artifact).forEach { field ->
            field.editText.isEnabled = hasToken
        }
        test.isEnabled = hasToken && hasRepo
        pull.isEnabled = hasToken && hasRepo
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
                status.text = "Selected ${selected.fileName}. Expand repo details to review build settings."
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
                val apk = client.downloadArtifactApk(latestArtifact, cacheDir.resolve("compandroid"))
                val validation = ApkValidator.validateUpdate(this, apk, config.packageName)
                require(validation.ok) { validation.message }
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
        text = "Compandroid Settings"
        textSize = 24f
        setPadding(0, 0, 0, 8)
    }

    private fun description(): TextView = TextView(this).apply {
        text = "Scan a desktop token QR or paste a read-only GitHub token, then choose the repository that publishes debug APKs."
        textSize = 14f
        setPadding(0, 0, 0, 24)
    }

    private fun sectionTitle(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 14f
        setPadding(0, 8, 0, 4)
    }

    private fun field(
        label: String,
        value: String,
        hidden: Boolean = false,
        cameraButton: Boolean = false
    ): TitledField {
        val title = TextView(this).apply {
            text = label
            textSize = 12f
            setPadding(0, 12, 0, 0)
        }
        val editText = EditText(this).apply {
            hint = label
            setText(value)
            setSingleLine(true)
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
                setPadding(24, 24, 24, 24)
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
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

    private data class TitledField(
        val label: String,
        val container: LinearLayout,
        val editText: EditText,
        val actionButton: ImageButton? = null
    )
}
