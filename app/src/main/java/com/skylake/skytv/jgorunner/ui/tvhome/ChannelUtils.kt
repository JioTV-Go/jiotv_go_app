package com.skylake.skytv.jgorunner.ui.tvhome

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone


object ChannelUtils {
    // Load canonical JioTV channels: cache first then network
    suspend fun loadCanonicalChannels(context: Context, port: Int): List<Channel> {
        // cache
        loadFromCache(context)?.let { if (it.isNotEmpty()) return it }
        // network
        return try {
            fetchChannels("http://localhost:$port/channels")?.result ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadFromCache(context: Context): List<Channel>? {
        return try {
            val prefs = context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
            val json = prefs.getString("channels_json", null)
            if (json.isNullOrBlank()) {
                null
            } else {
                val resp = Gson().fromJson(json, ChannelResponse::class.java)
                resp?.result ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun fetchChannels(urlString: String, save2pref: Boolean = false,   context: Context? = null): ChannelResponse? {

        val gson = Gson()
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()

                    if (save2pref) {
                        val responseJsonString = Gson().toJson(response)
                        val sharedPref = context?.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
                        sharedPref?.edit {
                            putString("channels_json", responseJsonString)
                        }
                    }

                    gson.fromJson(response, ChannelResponse::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FetchChannels", "Error fetching channels", e)
            null
        }
    }

    fun filterChannels(
        channelsResponse: ChannelResponse?,
        languageIds: List<Int>? = null,
        categoryIds: List<Int>? = null,
        isHD: Boolean? = null
    ): List<Channel> {
        return channelsResponse?.result?.filter { channel ->
            (languageIds == null || languageIds.contains(channel.channelLanguageId)) &&
                    (categoryIds == null || categoryIds.contains(channel.channelCategoryId)) &&
                    (isHD == null || channel.isHD == isHD)
        } ?: emptyList()
    }

    // Function to fetch EPG data
    suspend fun fetchEpg(urlString: String): EpgProgram? {
        return withContext(Dispatchers.IO) {
            try {
//                Log.d("EPG_URL", urlString)
                val json = URL(urlString).readText()
                val epgResponse = Gson().fromJson(json, EpgResponse::class.java)

                val currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

                epgResponse.epg.find { program ->
                    currentTime >= program.startEpoch && currentTime <= program.endEpoch
                }
            } catch (e: Exception) {
                Log.e("EPG_FETCH", "Error fetching EPG data: ${e.message}")
                null
            }
        }
    }
}


