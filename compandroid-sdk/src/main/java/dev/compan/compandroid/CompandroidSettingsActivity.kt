package dev.compan.compandroid

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

class CompandroidSettingsActivity : Activity() {
    private lateinit var prefs: CompandroidPrefs
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CompandroidPrefs(this)
        title = "Compandroid"

        val owner = field("Owner", prefs.owner)
        val repo = field("Repo", prefs.repo)
        val branch = field("Branch", prefs.branch)
        val artifact = field("Artifact", prefs.artifactName)
        val token = field("GitHub read token", prefs.token)
        status = TextView(this).apply {
            text = "Ready"
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }

        val save = Button(this).apply {
            text = "Save Settings"
            setOnClickListener {
                prefs.owner = owner.text.toString().trim()
                prefs.repo = repo.text.toString().trim()
                prefs.branch = branch.text.toString().trim()
                prefs.artifactName = artifact.text.toString().trim()
                prefs.token = token.text.toString().trim()
                status.text = "Settings saved"
            }
        }

        val pull = Button(this).apply {
            text = "Pull Latest APK"
            setOnClickListener {
                save.performClick()
                pullLatest()
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(heading())
            addView(owner)
            addView(repo)
            addView(branch)
            addView(artifact)
            addView(token)
            addView(save)
            addView(pull)
            addView(status)
        }

        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun pullLatest() {
        status.text = "Checking GitHub Actions..."
        thread {
            runCatching {
                val config = prefs.config(packageName)
                require(config.owner.isNotBlank() && config.repo.isNotBlank()) {
                    "Owner and repo are required"
                }
                val client = GitHubActionsClient(prefs.token)
                val artifact = client.latestSuccessfulArtifact(config)
                    ?: error("No matching successful APK artifact found")
                val apk = client.downloadArtifactApk(artifact, cacheDir.resolve("compandroid"))
                val validation = ApkValidator.validateUpdate(this, apk, config.packageName)
                require(validation.ok) { validation.message }
                runOnUiThread {
                    status.text = "${validation.message}. Installing ${artifact.headSha.take(7)}"
                    ApkInstaller.install(this, apk)
                }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Pull failed" }
            }
        }
    }

    private fun heading(): TextView = TextView(this).apply {
        text = "Compandroid Settings"
        textSize = 24f
        setPadding(0, 0, 0, 24)
    }

    private fun field(label: String, value: String): EditText = EditText(this).apply {
        hint = label
        setText(value)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
