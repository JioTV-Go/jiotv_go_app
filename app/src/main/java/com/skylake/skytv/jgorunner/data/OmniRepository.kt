package com.skylake.skytv.jgorunner.data

import android.content.Context
import android.util.Log
import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OmniRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchChannels(port: Int = 5350): List<OmniChannel> = withContext(Dispatchers.IO) {
        try {
            val url = "http://127.0.0.1:$port/playlist.m3u"
            val request = Request.Builder().url(url).build()
            val m3uChannels = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                parseM3U(response.body?.string() ?: "")
            }

            if (m3uChannels.isEmpty()) return@withContext emptyList()

            // Fetch JSON /channels for requiresSubscription
            try {
                val jsonRequest = Request.Builder().url("http://127.0.0.1:$port/channels").build()
                client.newCall(jsonRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonBody = response.body?.string() ?: return@use
                        val jsonObj = com.google.gson.JsonParser.parseString(jsonBody).asJsonObject
                        val resultArray = jsonObj.getAsJsonArray("result") ?: return@use
                        val subscriptionMap = mutableMapOf<String, Boolean>()
                        for (el in resultArray) {
                            val obj = el.asJsonObject
                            val id = obj.get("channel_id")?.asInt?.toString() ?: continue
                            val requiresSub = obj.get("requiresSubscription")?.asBoolean ?: false
                            if (requiresSub) subscriptionMap[id] = true
                        }
                        if (subscriptionMap.isNotEmpty()) {
                            return@withContext m3uChannels.map { ch ->
                                if (ch.id != null && subscriptionMap[ch.id] == true) {
                                    ch.copy(requiresSubscription = true)
                                } else ch
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("OmniRepository", "Failed to fetch subscription info: ${e.message}")
            }

            m3uChannels
        } catch (e: Exception) {
            Log.e("OmniRepository", "Error", e)
            emptyList()
        }
    }

    private fun parseM3U(body: String): List<OmniChannel> {
        val channels = mutableListOf<OmniChannel>()
        val lines = body.split("\n")
        var name: String? = null
        var logo: String? = null
        var group: String? = null
        var language: String? = null
        var url: String? = null

        val languages = listOf("Hindi", "English", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali", "Marathi", "Gujarati", "Punjabi", "Urdu", "Odia", "Assamese")

        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#EXTINF")) {
                val nm = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                name = nm?.groupValues?.get(1) ?: t.split(",").lastOrNull()?.trim()
                val lg = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                logo = lg?.groupValues?.get(1)
                val gr = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                group = gr?.groupValues?.get(1)
                
                val ln = Regex("""tvg-language="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                    ?: Regex("""language="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                    ?: Regex("""tvg-lang="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                language = ln?.groupValues?.get(1)
            } else if (t.startsWith("http") && name != null) {
                url = t
                val extractedId = url.substringAfterLast("/").substringBefore(".").trim()

                // Derive MPD and key URLs for the local JioTV Go server.
                // HLS: http://127.0.0.1:{port}/live/{id}.m3u8
                // MPD: http://127.0.0.1:{port}/live/mpd/{id}.mpd (DRM DASH manifest)
                // Key: http://127.0.0.1:{port}/live/key/{id} (Widevine license)
                val isLocalJio = url.contains("127.0.0.1") || url.contains("localhost")
                val derivedMpdUrl = if (isLocalJio && url.contains("/live/") && url.endsWith(".m3u8")) {
                    url.substringBefore("/live/") + "/live/mpd/" + extractedId + ".mpd"
                } else null
                val derivedKeyUrl = if (isLocalJio && url.contains("/live/") && url.endsWith(".m3u8")) {
                    url.substringBefore("/live/") + "/live/key/" + extractedId
                } else null

                
                if (language.isNullOrBlank()) {
                    val combinedText = "${group ?: ""} ${name ?: ""}".lowercase()
                    for (l in languages) {
                        if (combinedText.contains(l.lowercase())) {
                            language = l
                            break
                        }
                    }
                }

                channels.add(OmniChannel(
                    id = extractedId, 
                    name = name, 
                    group = group, 
                    language = language, 
                    logo = logo, 
                    url = url, 
                    m3u8Url = url, 
                    mpdUrl = derivedMpdUrl,
                    licenseUrl = derivedKeyUrl
                ))
                name = null; logo = null; group = null; language = null; url = null
            }
        }
        return channels
    }
}
