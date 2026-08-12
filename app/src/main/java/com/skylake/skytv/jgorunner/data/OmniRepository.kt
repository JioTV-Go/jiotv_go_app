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
            val url = "http://localhost:$port/playlist.m3u"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                parseM3U(response.body?.string() ?: "")
            }
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
        var url: String? = null

        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#EXTINF")) {
                val nm = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                name = nm?.groupValues?.get(1) ?: t.split(",").lastOrNull()?.trim()
                val lg = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                logo = lg?.groupValues?.get(1)
                val gr = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE).find(t)
                group = gr?.groupValues?.get(1)
            } else if (t.startsWith("http") && name != null) {
                url = t
                val extractedId = url.substringAfterLast("/").substringBefore(".").trim()
                channels.add(OmniChannel(id = extractedId, name = name, group = group, logo = logo, url = url, m3u8Url = url, mpdUrl = null))
                name = null; logo = null; group = null; url = null
            }
        }
        return channels
    }
}
