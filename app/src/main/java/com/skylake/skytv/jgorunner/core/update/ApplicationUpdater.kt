package com.skylake.skytv.jgorunner.core.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cthing.versionparser.semver.SemanticVersion
import org.json.JSONObject
import java.net.URL

object ApplicationUpdater {
    private const val TAG = "JTVGo::AppUpdater"

    private const val LATEST_RELEASE_INFO_URL =
        "https://api.github.com/repos/JioTV-Go/jiotv_go_app/releases/latest"

    suspend fun fetchLatestReleaseInfo() =
        withContext(Dispatchers.IO) {
            try {
                val response = URL(LATEST_RELEASE_INFO_URL).readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")

                val releaseTargetDetails: JSONObject? = try {
                    assets.getJSONObject(0)
                } catch (e: Exception) {
                    null
                }

                if (releaseTargetDetails == null) {
                    Log.e(TAG, "No release found!")
                    return@withContext null
                }

                val assetName = releaseTargetDetails.getString("name")
                val downloadUrl = releaseTargetDetails.getString("browser_download_url")
                val downloadSize = releaseTargetDetails.getLong("size")
                val version = SemanticVersion.parse(tagName)
                return@withContext DownloadAsset(assetName, version, downloadUrl, downloadSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest release info: ${e.message}")
                return@withContext null
            }
        }
}