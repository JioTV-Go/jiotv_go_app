package com.skylake.skytv.jgorunner.core.update

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cthing.versionparser.semver.SemanticVersion
import org.json.JSONObject
import java.net.URL

object BinaryUpdater {
    private const val TAG = "JTVGo::BinaryUpdater"

    private const val LATEST_RELEASE_INFO_URL =
        "https://api.github.com/repos/JioTV-Go/jiotv_go/releases/latest"
    private const val RELEASE_NAME_PREFIX = "jiotv_go-android"

    suspend fun fetchLatestReleaseInfo() =
        withContext(Dispatchers.IO) {
            try {
                val response = URL(LATEST_RELEASE_INFO_URL).readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                var releaseTargetDetails: JSONObject? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)

                    // Get system arch (amd64 or arm or arm64 or armv7)
                    val supportedABIs = Build.SUPPORTED_ABIS
                    val releaseNameSuffix = when {
                        supportedABIs.contains("arm64-v8a") -> "-arm64"
                        supportedABIs.contains("armeabi-v7a") -> "5-armv7"
                        supportedABIs.contains("armeabi") -> "-arm"
                        supportedABIs.contains("x86_64") -> "-amd64"
                        else -> "-arm"
                    }

                    if (asset.getString("name").contains(RELEASE_NAME_PREFIX + releaseNameSuffix)) {
                        releaseTargetDetails = asset
                        break
                    }
                }

                if (releaseTargetDetails == null) {
                    Log.e(TAG, "No release found for the current system architecture")
                    return@withContext null
                }
                val assetName = releaseTargetDetails.getString("name")
                val downloadUrl = releaseTargetDetails.getString("browser_download_url")
                val downloadSize = releaseTargetDetails.getLong("size")
                val version = SemanticVersion.parse(tagName)
                Log.d(TAG, "Latest release info: $version, $assetName, $downloadUrl, $downloadSize")
                return@withContext DownloadAsset(assetName, version, downloadUrl, downloadSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest release info: ${e.message}")
                return@withContext null
            }
        }
}

