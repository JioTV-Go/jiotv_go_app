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
    private const val RELEASE_NAME_PREFIX = "jiotv_go-android"
    private lateinit var sharedPref: SkySharedPref

    /**
     * Why the last fetchLatestReleaseInfo() returned null, so the UI can show the real
     * cause instead of always blaming the network. Null means "succeeded / not yet run".
     */
    @Volatile
    var lastFailureReason: String? = null
        private set

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
                val version = SemanticVersionNew.parseOrDefault(prefs.cachedReleaseVersion)
                return@withContext DownloadAsset(
                    name = prefs.cachedReleaseName!!,
                    version = version,
                    downloadUrl = prefs.cachedReleaseUrl!!,
                    downloadSize = prefs.cachedReleaseSize
                )
            }
        }

        try {
            lastFailureReason = null
            val url = "https://api.github.com/repos/JioTV-Go/jiotv_go/releases/latest"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val tagName = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            var releaseTargetDetails: JSONObject? = null

            val supportedABIs = Build.SUPPORTED_ABIS.map { it.lowercase() }

            // Build an ordered list of acceptable asset suffixes. Native match first, then —
            // for Intel/x86 devices — ARM builds as a fallback, since most x86 Android
            // devices can run ARM executables via the houdini translation layer.
            val suffixPriority = LinkedHashSet<String>()
            for (abi in supportedABIs) {
                when (abi) {
                    "arm64-v8a" -> suffixPriority.add("-arm64")
                    "armeabi-v7a" -> suffixPriority.add("-armv7")
                    "armeabi" -> suffixPriority.add("-arm")
                    "x86_64" -> suffixPriority.add("-amd64")
                    "x86" -> suffixPriority.add("-386")
                }
            }
            if (supportedABIs.contains("x86_64")) {
                suffixPriority.addAll(listOf("-arm64", "-armv7", "-arm"))
            }
            if (supportedABIs.contains("x86")) {
                suffixPriority.addAll(listOf("-armv7", "-arm"))
            }
            if (suffixPriority.isEmpty()) suffixPriority.add("-armv7")

            // Match a suffix as a whole token so "-arm" doesn't match "...-arm64"/"...-armv7".
            fun assetMatches(name: String, suffix: String): Boolean {
                if (!name.contains(RELEASE_NAME_PREFIX, ignoreCase = true)) return false
                val idx = name.indexOf(suffix, ignoreCase = true)
                if (idx < 0) return false
                val after = idx + suffix.length
                return after >= name.length || !name[after].isLetterOrDigit()
            }

            var chosenSuffix: String? = null
            outer@ for (suffix in suffixPriority) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (assetMatches(asset.optString("name"), suffix)) {
                        releaseTargetDetails = asset
                        chosenSuffix = suffix
                        break@outer
                    }
                }
            }

            if (releaseTargetDetails == null) {
                // GitHub WAS reachable — the release just doesn't ship a build for this CPU.
                val available = (0 until assets.length())
                    .map { assets.getJSONObject(it).optString("name") }
                    .filter { it.contains(RELEASE_NAME_PREFIX) }
                lastFailureReason = "No server build for this device CPU " +
                        "(device ABIs=${supportedABIs.joinToString()}, tried ${suffixPriority.joinToString()}). " +
                        "Release $tagName has: ${available.joinToString().ifBlank { "no android binaries" }}"
                Log.e(TAG, lastFailureReason!!)
                return@withContext null
            }
            Log.d(TAG, "Selected binary asset via suffix '$chosenSuffix' for ABIs=$supportedABIs")

            val assetName = releaseTargetDetails.getString("name")
            val downloadUrl = releaseTargetDetails.getString("browser_download_url")
            val downloadSize = releaseTargetDetails.getLong("size")
            val version = SemanticVersionNew.parseOrDefault(tagName)

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
            lastFailureReason = "Couldn't reach GitHub: ${e.message ?: e.javaClass.simpleName}"
            Log.e(TAG, "Error fetching latest release info: ${e.message}")
            return@withContext null
        }
    }
}
