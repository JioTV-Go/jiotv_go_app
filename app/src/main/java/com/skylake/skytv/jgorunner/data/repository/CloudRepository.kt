package com.skylake.skytv.jgorunner.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skylake.skytv.jgorunner.data.model.CloudChannel
import com.skylake.skytv.jgorunner.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CloudRepository(private val context: Context) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val sharedPrefs = context.getSharedPreferences("cloud_ui_cache", Context.MODE_PRIVATE)

    suspend fun fetchServers(): List<Server> = withContext(Dispatchers.IO) {
        val url = "https://cloudplay-app-json.pages.dev/cat/jiotv+.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<Server>>() {}.type
                gson.fromJson<List<Server>>(body, type)
            }
        } catch (e: Exception) {
            Log.e("CloudRepository", "Error fetching servers", e)
            emptyList()
        }
    }

    suspend fun fetchChannels(serverUrl: String, forceRefresh: Boolean = false): List<CloudChannel> = withContext(Dispatchers.IO) {
        val cacheKey = "channels_${serverUrl.hashCode()}"
        val cacheTimeKey = "${cacheKey}_time"

        if (!forceRefresh) {
            val lastFetch = sharedPrefs.getLong(cacheTimeKey, 0L)
            val now = System.currentTimeMillis()
            if (now - lastFetch < TimeUnit.HOURS.toMillis(1)) {
                val cachedData = sharedPrefs.getString(cacheKey, null)
                if (!cachedData.isNullOrEmpty()) {
                    try {
                        val type = object : TypeToken<List<CloudChannel>>() {}.type
                        return@withContext gson.fromJson<List<CloudChannel>>(cachedData, type)
                    } catch (e: Exception) {
                        Log.e("CloudRepository", "Error parsing cached channels", e)
                    }
                }
            }
        }

        try {
            val request = Request.Builder().url(serverUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<CloudChannel>>() {}.type
                val channels = gson.fromJson<List<CloudChannel>>(body, type)

                sharedPrefs.edit()
                    .putString(cacheKey, body)
                    .putLong(cacheTimeKey, System.currentTimeMillis())
                    .apply()

                channels
            }
        } catch (e: Exception) {
            Log.e("CloudRepository", "Error fetching channels from $serverUrl", e)
            emptyList()
        }
    }
}
