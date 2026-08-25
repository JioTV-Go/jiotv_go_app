package com.skylake.skytv.jgorunner.core.update

import android.content.Context
import android.os.Build
import android.util.Log
import com.skylake.skytv.jgorunner.data.SkySharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object BinaryUpdater {
    private const val TAG = "JTVGo::BinaryUpdater"
    private const val LATEST_RELEASE_INFO_URL = "https://api.github.com/repos/JioTV-Go/jiotv_go/releases/latest"
    private const val RELEASE_NAME_PREFIX = "jiotv_go-android"
    private lateinit var sharedPref: SkySharedPref

    fun init(context: Context) {
        sharedPref = SkySharedPref.getInstance(context)
    }

    suspend fun fetchLatestReleaseInfo(): DownloadAsset? = withContext(Dispatchers.IO) {
        val prefs = sharedPref.myPrefs
        val currentTime = System.currentTimeMillis()
        val cacheValidity = 5 * 60 * 1000L // 5 minutes

        // Check cache validity
        if (prefs.lastFetchTimeRelease != 0L && (currentTime - prefs.lastFetchTimeRelease) < cacheValidity) {
            if (!prefs.cachedReleaseName.isNullOrEmpty() && !prefs.cachedReleaseVersion.isNullOrEmpty() && !prefs.cachedReleaseUrl.isNullOrEmpty()) {
                Log.d(TAG, "Returning cached release info from SharedPreferences")
                val version = SemanticVersionNew.parse(prefs.cachedReleaseVersion!!)
                return@withContext DownloadAsset(
                    name = prefs.cachedReleaseName!!,
                    version = version,
                    downloadUrl = prefs.cachedReleaseUrl!!,
                    downloadSize = prefs.cachedReleaseSize
                )
            }
        }

        try {
            val response = URL(LATEST_RELEASE_INFO_URL).readText()
            val json = JSONObject(response)
            val tagName = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            var releaseTargetDetails: JSONObject? = null

            val supportedABIs = Build.SUPPORTED_ABIS
            val releaseNameSuffix = when {
                supportedABIs.contains("arm64-v8a") -> "-arm64"
                supportedABIs.contains("armeabi-v7a") -> "5-armv7"
                supportedABIs.contains("armeabi") -> "-arm"
                supportedABIs.contains("x86_64") -> "-amd64"
                else -> "5-armv7"
            }

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").contains(RELEASE_NAME_PREFIX + releaseNameSuffix)) {
                    releaseTargetDetails = asset
                    break
                }
            }

            if (releaseTargetDetails == null) {
                Log.e(TAG, "No release found for current system architecture")
                return@withContext null
            }

            val assetName = releaseTargetDetails.getString("name")
            val downloadUrl = releaseTargetDetails.getString("browser_download_url")
            val downloadSize = releaseTargetDetails.getLong("size")
            val version = SemanticVersionNew.parse(tagName)

            // Cache in SharedPreferences
            prefs.cachedReleaseName = assetName
            prefs.cachedReleaseVersion = tagName
            prefs.cachedReleaseUrl = downloadUrl
            prefs.cachedReleaseSize = downloadSize
            prefs.lastFetchTimeRelease = currentTime
            sharedPref.savePreferences()

            Log.d(TAG, "Latest release fetched and cached in SharedPreferences")
            return@withContext DownloadAsset(assetName, version, downloadUrl, downloadSize)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest release info: ${e.message}")
            return@withContext null
        }
    }
}
