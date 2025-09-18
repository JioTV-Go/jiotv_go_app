package com.skylake.skytv.jgorunner.ui.tvhome.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.tvhome.M3UChannelExp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessingDialogExp(
    context: Context,
    onComplete: (List<M3UChannelExp>) -> Unit,
    onError: (String) -> Unit
) {
    val preferenceManager = SkySharedPref.getInstance(context)
    var message by remember { mutableStateOf("Processing...") }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val m3uUrl = preferenceManager.myPrefs.custURL
            Log.d("ProcessingDialog", "Starting processing. M3U URL: $m3uUrl")
            if (m3uUrl.isNullOrBlank()) {
                message = "No M3U URL found."
                isLoading = false
                Log.e("ProcessingDialog", "No M3U URL found in preferences.")
                onError(message)
                return@launch
            }
            try {
                // Fetch the M3U file
                val response = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(m3uUrl).build()
                    Log.d("ProcessingDialog", "Fetching M3U file from URL...")
                    client.newCall(request).execute()
                }

                val body = response.body?.string()

                if (!response.isSuccessful || body == null) {
                    message = "Failed to fetch M3U file. HTTP error: ${response.code}"
                    isLoading = false
                    Log.e("ProcessingDialog", "Failed to fetch M3U file. HTTP code: ${response.code}")
                    onError(message)
                    return@launch
                }
                if (!body.startsWith("#EXTM3U")) {
                    message = "Invalid M3U file format."
                    isLoading = false
                    Log.e("ProcessingDialog", "Fetched file does not start with #EXTM3U.")
                    onError(message)
                    return@launch
                }
                Log.d("ProcessingDialog", "M3U file fetched successfully. Parsing...")
                val channels = parseM3Uexp(body)
                Log.d("ProcessingDialog", "Parsed ${channels.size} channels from M3U.")
                val gson = Gson()
                val json = gson.toJson(channels)
                preferenceManager.myPrefs.channelListJson = json
                preferenceManager.savePreferences()
                Log.d("ProcessingDialog", "Channel list saved to preferences.")
                isLoading = false
                message = "Channels loaded: ${channels.size}"
                onComplete(channels)
            } catch (e: Exception) {
                message = "Error: ${e.localizedMessage}"
                isLoading = false
                Log.e("ProcessingDialog", "Exception during processing: ${e.localizedMessage}", e)
                onError(message)
            }
        }
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(message, fontSize = 16.sp)
            }
        }
    }
}

fun parseM3Uexp(m3uContent: String): List<M3UChannelExp> {
    val lines = m3uContent.lines()
    val channels = mutableListOf<M3UChannelExp>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.startsWith("#EXTINF")) {
            val name = Regex(",\\s*(.+)$").find(line)?.groupValues?.get(1)?.trim() ?: ""
            val logo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.get(1)
            val category = Regex("""group-title="([^"]*)"""").find(line)?.groupValues?.get(1)

            val url = lines.getOrNull(i + 1)?.trim() ?: ""

            if (name.isNotEmpty() && url.isNotEmpty()) {
                Log.d("parseM3Uexp", "Parsed channel: name=$name, url=$url, logo=$logo, category=$category")
                channels.add(M3UChannelExp(name, url, logo, category))
            }
            i += 2
        } else {
            i++
        }
    }
    Log.d("parseM3Uexp", "Total channels parsed: ${channels.size}")
    return channels
}
