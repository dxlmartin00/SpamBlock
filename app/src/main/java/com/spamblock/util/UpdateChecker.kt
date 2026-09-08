package com.spamblock.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null
)

object UpdateChecker {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/dxlmartin00/SpamBlock/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SpamBlock-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString(
                    "html_url",
                    "https://github.com/dxlmartin00/SpamBlock/releases/latest"
                )
                val body = json.optString("body", "").trim()

                var apkDownloadUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (isNewerVersion(tagName, currentVersion)) {
                    return@withContext UpdateInfo(
                        latestVersion = tagName,
                        releaseUrl = htmlUrl,
                        downloadUrl = apkDownloadUrl,
                        releaseNotes = if (body.isNotEmpty()) body else null
                    )
                }
            }
        } catch (_: Exception) {
            // Silently ignore network / offline failures with zero impact on caller
        }
        null
    }

    /**
     * Compares remote version tag (e.g. "v2.1.0", "v2.1") with currentVersion (e.g. "2.0.0").
     * Returns true if remote is strictly greater.
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        if (remoteTag.isBlank() || currentVersion.isBlank()) return false

        val remoteClean = remoteTag.trim().removePrefix("v").removePrefix("V")
        val currentClean = currentVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }

        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
