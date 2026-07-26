package dev.compan.compandroid

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class CompandroidAiSettingsActivity : Activity() {
    private lateinit var prefs: CompandroidPrefs
    private lateinit var provider: Spinner
    private lateinit var project: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = CompandroidPrefs(this)
        title = "AI Settings"

        val providers = listOf("ChatGPT", "Claude", "Other")
        provider = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CompandroidAiSettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                providers
            )
            setSelection(providers.indexOf(prefs.aiProvider).coerceAtLeast(0))
        }

        project = EditText(this).apply {
            hint = "Project name"
            setText(prefs.aiProjectName)
            setSingleLine(true)
        }

        val save = Button(this).apply {
            text = "Save AI settings"
            styleButton(Color.rgb(15, 130, 116), Color.WHITE)
            setOnClickListener {
                saveSettings()
                Toast.makeText(this@CompandroidAiSettingsActivity, "AI settings saved", Toast.LENGTH_SHORT).show()
            }
        }

        val shareScreenshot = Button(this).apply {
            text = "Share captured screenshot"
            styleButton(Color.rgb(194, 65, 12), Color.WHITE)
            setOnClickListener {
                saveSettings()
                shareScreenshot()
            }
        }

        val shareLogs = Button(this).apply {
            text = "Share app logs"
            styleOutlinedButton(Color.rgb(15, 130, 116))
            setOnClickListener {
                saveSettings()
                shareLogs()
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
            setPadding(dp(24), dp(32), dp(24), dp(32))
            addView(heading("AI Settings", 28f))
            addView(body("Choose the AI app used for development and the project name that should be included when sharing debugging context."), margin(top = dp(8)))
            addView(label("AI provider"), margin(top = dp(28)))
            addView(provider, margin(top = dp(8)))
            addView(label("Project name"), margin(top = dp(20)))
            addView(project, margin(top = dp(4)))
            addView(save, margin(top = dp(20)))
            addView(heading("Share to AI", 22f), margin(top = dp(36)))
            addView(body("CompanDROID opens a new share conversation in the selected app and includes the project name in the message. AI apps do not currently expose a standard Android API for selecting a specific internal project automatically."), margin(top = dp(8)))
            addView(shareScreenshot, margin(top = dp(20)))
            addView(shareLogs, margin(top = dp(12)))
        }

        val scroll = ScrollView(this).apply {
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
    }

    private fun saveSettings() {
        prefs.aiProvider = provider.selectedItem?.toString() ?: "Other"
        prefs.aiProjectName = project.text.toString().trim()
    }

    private fun shareScreenshot() {
        val file = Compandroid.pendingScreenshotFile(this)
        if (!file.isFile) {
            Toast.makeText(
                this,
                "No screenshot is ready. Return to the app and shake the device first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        shareFile(file, "image/png", "Please review this screenshot from ${projectLabel()} and help diagnose or improve the app.")
    }

    private fun shareLogs() {
        val file = File(cacheDir, "compandroid/compan-app-logs.txt")
        file.parentFile?.mkdirs()
        val logs = runCatching {
            val command = if (android.os.Build.VERSION.SDK_INT >= 24) {
                arrayOf("logcat", "-d", "--pid=${android.os.Process.myPid()}", "-t", "500")
            } else {
                arrayOf("logcat", "-d", "-t", "500")
            }
            Runtime.getRuntime().exec(command).inputStream.bufferedReader().use { it.readText() }
        }.getOrElse { error -> "Could not collect app logs: ${error.message}" }
        file.writeText(logs.ifBlank { "No app logs were available." })
        shareFile(file, "text/plain", "Please review these Android app logs from ${projectLabel()} and help diagnose the issue.")
    }

    private fun shareFile(file: File, mimeType: String, message: String) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.compandroid.files",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            clipData = ClipData.newUri(contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val targetPackage = when (prefs.aiProvider) {
            "ChatGPT" -> "com.openai.chatgpt"
            "Claude" -> "com.anthropic.claude"
            else -> null
        }
        if (targetPackage != null && packageManager.getLaunchIntentForPackage(targetPackage) != null) {
            send.setPackage(targetPackage)
            startActivity(send)
        } else {
            startActivity(Intent.createChooser(send, "Share to ${prefs.aiProvider}"))
        }
    }

    private fun projectLabel(): String = prefs.aiProjectName.ifBlank { "this development project" }

    private fun heading(textValue: String, size: Float) = TextView(this).apply {
        text = textValue
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 23, 42))
    }

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(15, 23, 42))
    }

    private fun body(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 15f
        setTextColor(Color.rgb(71, 85, 105))
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
        background = rounded(Color.WHITE, color, dp(12).toFloat())
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