package dev.compan.compandroid

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

internal class GitHubActionsClient(private val token: String) {
    fun repositories(): List<GitHubRepository> {
        val repositories = mutableListOf<GitHubRepository>()
        for (page in 1..3) {
            val pageItems = getJsonArray("https://api.github.com/user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&per_page=100&page=$page")
            for (index in 0 until pageItems.length()) {
                val repo = pageItems.getJSONObject(index)
                val owner = repo.getJSONObject("owner").getString("login")
                repositories += GitHubRepository(
                    fullName = repo.getString("full_name"),
                    owner = owner,
                    name = repo.getString("name"),
                    private = repo.getBoolean("private")
                )
            }
            if (pageItems.length() < 100) break
        }
        return repositories
    }

    fun workflows(owner: String, repo: String): List<GitHubWorkflow> {
        val workflows = mutableListOf<GitHubWorkflow>()
        for (page in 1..3) {
            val pageItems = getJson("https://api.github.com/repos/$owner/$repo/actions/workflows?per_page=100&page=$page")
                .getJSONArray("workflows")
            for (index in 0 until pageItems.length()) {
                val workflow = pageItems.getJSONObject(index)
                workflows += GitHubWorkflow(
                    name = workflow.getString("name"),
                    path = workflow.getString("path"),
                    state = workflow.getString("state")
                )
            }
            if (pageItems.length() < 100) break
        }
        return workflows
    }

    fun latestSuccessfulArtifact(config: CompandroidConfig): GitHubArtifact? {
        for (page in 1..3) {
            val runsUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/actions/runs" +
                "?branch=${config.branch}&status=success&per_page=25&page=$page"
            val runs = getJson(runsUrl).getJSONArray("workflow_runs")

            for (index in 0 until runs.length()) {
                val run = runs.getJSONObject(index)
                val workflowPath = run.optString("path")
                if (workflowPath.isNotBlank() && !workflowPath.endsWith(config.workflowFileName)) {
                    continue
                }

                val artifactsUrl = run.getString("artifacts_url")
                for (artifactPage in 1..3) {
                    val artifacts = getJson("$artifactsUrl?per_page=100&page=$artifactPage")
                        .getJSONArray("artifacts")
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

    private fun getJsonArray(url: String) = org.json.JSONArray(
        openConnection(url).inputStream.bufferedReader().use { it.readText() }
    )

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        if (connection.responseCode !in 200..299) {
            throw GitHubApiException(connection.errorMessage())
        }
        return connection
    }

    private fun HttpURLConnection.errorMessage(): String {
        val body = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val remaining = getHeaderField("X-RateLimit-Remaining")
        val reset = getHeaderField("X-RateLimit-Reset")

        return when (responseCode) {
            401 -> "GitHub token was rejected. Check that the token is valid and has read access."
            403 -> if (remaining == "0") {
                "GitHub rate limit exceeded. Try again after reset time $reset."
            } else {
                "GitHub access was forbidden. For private repos, use a token with Metadata, Contents, and Actions read access."
            }
            404 -> "GitHub repo, branch, workflow, or artifact was not found. Check owner/repo, branch, and private repo access."
            else -> "GitHub request failed: HTTP $responseCode $body"
        }
    }
}
