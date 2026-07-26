package dev.compan.compandroid

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

class CompandroidLandingActivity : Activity() {
    private lateinit var prefs: CompandroidPrefs
    private lateinit var status: TextView
    private lateinit var pullButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CompandroidPrefs(this)
        title = "CompanDROID"

        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, dp(12), 0, 0)
            text = if (Compandroid.hasPendingHostScreenshot(this@CompandroidLandingActivity)) {
                "The screen shown before CompanDROID opened is ready to save."
            } else {
                "No screenshot is ready. Return to your app and shake the device again to capture it before this interface opens."
            }
        }

        val githubButton = Button(this).apply {
            text = "Open GitHub settings\nManage token, repository, and more"
            styleButton(Color.rgb(15, 130, 116), Color.WHITE)
            setOnClickListener {
                startActivity(Intent(this@CompandroidLandingActivity, CompandroidSettingsActivity::class.java))
            }
        }

        val captureButton = Button(this).apply {
            text = "Save captured screenshot"
            styleOutlinedButton(Color.rgb(194, 65, 12))
            isEnabled = Compandroid.hasPendingHostScreenshot(this@CompandroidLandingActivity)
            setOnClickListener {
                Compandroid.captureHostScreenshot(this@CompandroidLandingActivity)
                    .onSuccess { name ->
                        Toast.makeText(this@CompandroidLandingActivity, "Screenshot saved: $name", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .onFailure { error ->
                        status.text = error.message ?: "Screenshot failed"
                        isEnabled = Compandroid.hasPendingHostScreenshot(this@CompandroidLandingActivity)
                    }
            }
        }

        pullButton = Button(this).apply {
            text = "Pull latest APK\nFetch the latest build from GitHub"
            styleButton(Color.rgb(15, 130, 116), Color.WHITE)
            setOnClickListener { pullLatest() }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
            setPadding(dp(24), dp(32), dp(24), dp(32))
            addView(TextView(this@CompandroidLandingActivity).apply {
                text = "CompanDROID"
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(15, 23, 42))
            })
            addView(TextView(this@CompandroidLandingActivity).apply {
                text = "A dev tool for building apps on Android."
                textSize = 16f
                setTextColor(Color.rgb(71, 85, 105))
                setPadding(0, dp(6), 0, dp(28))
            })
            addView(card(
                title = "GitHub Settings",
                description = "Configure GitHub integration and manage your repository.",
                action = githubButton
            ))
            addView(card(
                title = "Capture App Screenshot",
                description = "Save the app screen captured at the moment you shook the device, before CompanDROID opened.",
                action = captureButton
            ), margin(top = dp(16)))
            addView(status)
            addView(pullButton, margin(top = dp(24)))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(
                    view.paddingLeft,
                    view.paddingTop,
                    view.paddingRight,
                    dp(24) + insets.systemWindowInsetBottom
                )
                insets
            }
        }
        setContentView(scroll)
        scroll.requestApplyInsets()
        updatePullState()
    }

    override fun onResume() {
        super.onResume()
        if (::pullButton.isInitialized) updatePullState()
    }

    private fun updatePullState() {
        val config = prefs.config(packageName)
        pullButton.isEnabled = prefs.token.isNotBlank() && config.owner.isNotBlank() && config.repo.isNotBlank()
    }

    private fun pullLatest() {
        pullButton.isEnabled = false
        status.text = "Checking GitHub Actions..."
        thread {
            runCatching {
                val config = prefs.config(packageName)
                require(prefs.token.isNotBlank()) { "Add a GitHub token in GitHub settings first." }
                require(config.owner.isNotBlank() && config.repo.isNotBlank()) { "Choose a repository in GitHub settings first." }
                val client = GitHubActionsClient(prefs.token)
                val artifact = client.latestSuccessfulArtifact(config)
                    ?: error("No matching successful APK artifact found")
                val apk = client.downloadArtifactApk(artifact, cacheDir.resolve("compandroid"))
                val validation = ApkValidator.validateUpdate(this, apk, config.packageName)
                require(validation.ok) { validation.message }
                prefs.clearArtifactNotNewer()
                runOnUiThread {
                    val install = ApkInstaller.install(this, apk)
                    status.text = "${validation.message}. ${install.message} ${artifact.headSha.take(7)}"
                }
            }.onFailure { error ->
                runOnUiThread { status.text = error.message ?: "Pull failed" }
            }
            runOnUiThread { updatePullState() }
        }
    }

    private fun card(title: String, description: String, action: Button): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = rounded(Color.WHITE, Color.rgb(226, 232, 240), dp(16).toFloat())
        addView(TextView(this@CompandroidLandingActivity).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(15, 23, 42))
        })
        addView(TextView(this@CompandroidLandingActivity).apply {
            text = description
            textSize = 15f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, dp(8), 0, dp(16))
        })
        addView(action, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun Button.styleButton(backgroundColor: Int, textColor: Int) {
        isAllCaps = false
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor)
        background = rounded(backgroundColor, backgroundColor, dp(12).toFloat())
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    private fun Button.styleOutlinedButton(color: Int) {
        isAllCaps = false
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        background = rounded(Color.rgb(255, 247, 237), color, dp(12).toFloat())
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    private fun rounded(fill: Int, stroke: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        setStroke(dp(1), stroke)
        cornerRadius = radius
    }

    private fun margin(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
