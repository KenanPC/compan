package dev.compan.compandroid

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

internal class GitHubActionsClient(private val token: String) {
    fun latestSuccessfulArtifact(config: CompandroidConfig): GitHubArtifact? {
        val runsUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/actions/runs" +
            "?branch=${config.branch}&status=success&per_page=10"
        val runs = getJson(runsUrl).getJSONArray("workflow_runs")

        for (index in 0 until runs.length()) {
            val run = runs.getJSONObject(index)
            val artifactsUrl = run.getString("artifacts_url")
            val artifacts = getJson(artifactsUrl).getJSONArray("artifacts")
            for (artifactIndex in 0 until artifacts.length()) {
                val artifact = artifacts.getJSONObject(artifactIndex)
                if (!artifact.getBoolean("expired") && artifact.getString("name") == config.artifactName) {
                    return GitHubArtifact(
                        name = artifact.getString("name"),
                        downloadUrl = artifact.getString("archive_download_url"),
                        workflowRunId = run.getLong("id"),
                        headSha = run.getString("head_sha")
                    )
                }
            }
        }

        return null
    }

    fun downloadArtifactApk(artifact: GitHubArtifact, outputDir: File): File {
        outputDir.mkdirs()
        val connection = openConnection(artifact.downloadUrl)
        val apkFile = File(outputDir, "${artifact.name}.apk")

        ZipInputStream(BufferedInputStream(connection.inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                    apkFile.outputStream().use { output -> zip.copyTo(output) }
                    return apkFile
                }
                entry = zip.nextEntry
            }
        }

        error("Artifact ${artifact.name} did not contain an APK")
    }

    private fun getJson(url: String): JSONObject {
        val connection = openConnection(url)
        return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        if (connection.responseCode !in 200..299) {
            val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            error("GitHub request failed: HTTP ${connection.responseCode} $body")
        }
        return connection
    }
}

