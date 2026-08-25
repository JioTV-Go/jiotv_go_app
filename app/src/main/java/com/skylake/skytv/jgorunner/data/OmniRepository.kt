package com.skylake.skytv.jgorunner.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import com.skylake.skytv.jgorunner.utils.LogCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class OmniRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cacheFile = File(context.cacheDir, "omni_channels_cache.json")
    private val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours cache

    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
                LogCollector.log("OmniRepository: Cache cleared successfully")
            }
        } catch (e: Exception) {
            LogCollector.logError("OmniRepository: Failed to clear cache", e)
        }
    }

    suspend fun fetchChannels(port: Int = 5350, forceRefresh: Boolean = false): List<OmniChannel> = withContext(Dispatchers.IO) {
        try {
            // Check 6-hour local cache
            if (!forceRefresh && cacheFile.exists()) {
                val age = System.currentTimeMillis() - cacheFile.lastModified()
                if (age < CACHE_TTL_MS) {
                    try {
                        val cachedText = cacheFile.readText()
                        val type = object : TypeToken<List<OmniChannel>>() {}.type
                        val cachedList = Gson().fromJson<List<OmniChannel>>(cachedText, type)
                        if (!cachedList.isNullOrEmpty()) {
                            LogCollector.log("OmniRepository: Loaded ${cachedList.size} channels from 6-hour cache (age: ${age / 1000}s)")
                            return@withContext cachedList
                        }
                    } catch (e: Exception) {
                        LogCollector.log("OmniRepository: Cache read failed (${e.message}), fetching fresh from server...")
                    }
                }
            }

            val url = "http://127.0.0.1:$port/playlist.m3u"
            LogCollector.log("OmniRepository: Fetching playlist from $url (forceRefresh: $forceRefresh)")
            val request = Request.Builder().url(url).build()
            val m3uChannels = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    LogCollector.log("OmniRepository: M3U request failed with code ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: ""
                parseM3U(body, "http://127.0.0.1:$port")
            }

            if (m3uChannels.isEmpty()) {
                LogCollector.log("OmniRepository: Parsed 0 channels from M3U")
                return@withContext emptyList()
            }
            LogCollector.log("OmniRepository: Parsed ${m3uChannels.size} channels from M3U playlist")

            // Fetch JSON /channels for requiresSubscription
            var finalChannels = m3uChannels
            try {
                val jsonRequest = Request.Builder().url("http://127.0.0.1:$port/channels").build()
                client.newCall(jsonRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonBody = response.body?.string() ?: return@use
                        val jsonObj = JsonParser.parseString(jsonBody).asJsonObject
                        val resultArray = jsonObj.getAsJsonArray("result") ?: return@use
                        val subscriptionMap = mutableMapOf<String, Boolean>()
                        for (el in resultArray) {
                            val obj = el.asJsonObject
                            val id = obj.get("channel_id")?.asInt?.toString() ?: continue
                            val requiresSub = obj.get("requiresSubscription")?.asBoolean ?: false
                            if (requiresSub) subscriptionMap[id] = true
                        }
                        if (subscriptionMap.isNotEmpty()) {
                            LogCollector.log("OmniRepository: Enriched ${subscriptionMap.size} premium channels with subscription requirements")
                            finalChannels = m3uChannels.map { ch ->
                                val requiresSub = ch.id != null && subscriptionMap[ch.id] == true
                                ch.copy(requiresSubscription = requiresSub)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LogCollector.log("OmniRepository: Subscription info check note: ${e.message}")
            }

            // Save to 6-hour cache
            if (finalChannels.isNotEmpty()) {
                try {
                    cacheFile.writeText(Gson().toJson(finalChannels))
                    LogCollector.log("OmniRepository: Saved ${finalChannels.size} channels to 6-hour cache")
                } catch (e: Exception) {
                    LogCollector.logError("OmniRepository: Failed to save channels to cache", e)
                }
            }

            finalChannels
        } catch (e: Exception) {
            LogCollector.logError("OmniRepository: Error fetching channels", e)
            emptyList()
        }
    }

    private fun parseM3U(body: String, localBaseUrl: String): List<OmniChannel> {
        val channels = mutableListOf<OmniChannel>()
        val lines = body.trimStart('\uFEFF').split("\n")
        var name: String? = null
        var logo: String? = null
        var group: String? = null
        var language: String? = null
        var licenseUrl: String? = null
        var manifestType: String? = null
        var requiresSubscription = false
        val headers = mutableMapOf<String, String>()

        val languages = listOf("Hindi", "English", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali", "Marathi", "Gujarati", "Punjabi", "Urdu", "Odia", "Assamese")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val nm = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                name = nm?.groupValues?.get(1)?.trim() ?: line.split(",").lastOrNull()?.trim()
                val lg = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                logo = lg?.groupValues?.get(1)?.trim()
                val gr = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                group = gr?.groupValues?.get(1)?.trim()

                val ln = Regex("""tvg-language="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                    ?: Regex("""language="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                    ?: Regex("""tvg-lang="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                language = ln?.groupValues?.get(1)?.trim()

                val subMatch = Regex("""tvg-requires_subscription="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                    ?: Regex("""tvg-subscription="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
                requiresSubscription = subMatch?.groupValues?.get(1).equals("true", true) || subMatch?.groupValues?.get(1) == "1"
            } else if (line.startsWith("#KODIPROP", ignoreCase = true)) {
                val kv = line.substringAfter(":", "").trim()
                val propKey = kv.substringBefore("=").trim()
                val propVal = kv.substringAfter("=", "").trim()
                if (propKey.equals("inputstream.adaptive.license_key", true) && propVal.isNotBlank()) {
                    licenseUrl = propVal
                } else if (propKey.equals("inputstream.adaptive.manifest_type", true)) {
                    if (propVal.equals("mpd", true) || propVal.equals("dash", true)) {
                        manifestType = "dash"
                    }
                }
            } else if (line.startsWith("#EXTVLCOPT", ignoreCase = true)) {
                val match = Regex("""#EXTVLCOPT:(http-user-agent|user-agent|http-referrer|referrer|referer|http-origin|origin)=(.+)""", RegexOption.IGNORE_CASE).find(line)
                if (match != null) {
                    val k = match.groupValues[1].trim().lowercase()
                    val v = match.groupValues[2].trim()
                    when {
                        k.contains("user-agent") -> headers["User-Agent"] = v
                        k.contains("referer") || k.contains("referrer") -> headers["Referer"] = v
                        k.contains("origin") -> headers["Origin"] = v
                    }
                }
            } else if (line.startsWith("http", ignoreCase = true) && name != null) {
                var streamUrl = line
                if (streamUrl.contains("|")) {
                    val parts = streamUrl.split("|", limit = 2)
                    streamUrl = parts[0].trim()
                    val headersStr = parts.getOrNull(1)?.trim().orEmpty()
                    if (headersStr.isNotBlank()) {
                        headersStr.split("&").forEach { param ->
                            val kv = param.split("=", limit = 2)
                            val k = kv.getOrNull(0)?.trim().orEmpty()
                            val v = kv.getOrNull(1)?.trim().orEmpty()
                            if (k.isNotBlank() && v.isNotBlank()) {
                                when (k.lowercase()) {
                                    "license", "drmlicense", "drm_license" -> licenseUrl = v
                                    "user-agent", "http-user-agent" -> headers["User-Agent"] = v
                                    "origin" -> headers["Origin"] = v
                                    "referer", "referrer" -> headers["Referer"] = v
                                    "cookie" -> headers["Cookie"] = v
                                    else -> headers[k] = v
                                }
                            }
                        }
                    }
                }

                val extractedId = streamUrl
                    .substringAfterLast("/")
                    .substringBefore("?")
                    .substringBefore(".")
                    .trim()
                    .ifBlank { name.hashCode().toString() }

                val isLocalJio = streamUrl.contains("127.0.0.1") || streamUrl.contains("localhost")
                val isLiveOrPlay = streamUrl.contains("/live/") || streamUrl.contains("/play/")
                val base = if (streamUrl.contains("/live/")) streamUrl.substringBefore("/live/") 
                           else if (streamUrl.contains("/play/")) streamUrl.substringBefore("/play/")
                           else localBaseUrl

                // Local JioTV Go binary MPD manifest endpoint is /live/mpd/{id} (NO .mpd extension!)
                val derivedMpdUrl = if (isLocalJio && isLiveOrPlay) {
                    "$base/live/mpd/$extractedId"
                } else if (manifestType == "dash" || streamUrl.contains(".mpd", true)) {
                    streamUrl
                } else null

                val derivedKeyUrl = licenseUrl ?: if (isLocalJio && isLiveOrPlay) {
                    "$base/live/key/$extractedId"
                } else null

                val resolvedLogo = when {
                    logo.isNullOrBlank() -> ""
                    logo.startsWith("http", ignoreCase = true) -> logo
                    isLocalJio -> "$base/jtvimage/$logo"
                    else -> logo
                }

                if (language.isNullOrBlank()) {
                    val combinedText = "${group ?: ""} $name".lowercase()
                    for (l in languages) {
                        if (combinedText.contains(l.lowercase())) {
                            language = l
                            break
                        }
                    }
                }

                channels.add(
                    OmniChannel(
                        id = extractedId,
                        name = name,
                        group = group?.ifBlank { "General" } ?: "General",
                        language = language?.ifBlank { "Hindi" } ?: "Hindi",
                        logo = resolvedLogo,
                        url = streamUrl,
                        m3u8Url = streamUrl,
                        mpdUrl = derivedMpdUrl,
                        licenseUrl = derivedKeyUrl,
                        headers = headers.takeIf { it.isNotEmpty() },
                        requiresSubscription = requiresSubscription
                    )
                )

                name = null; logo = null; group = null; language = null; licenseUrl = null; manifestType = null; requiresSubscription = false
                headers.clear()
            }
        }
        return channels
    }
}
