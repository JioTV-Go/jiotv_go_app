package com.skylake.skytv.jgorunner.core.update

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                } catch (_: Exception) {
                    null
                }

                if (releaseTargetDetails == null) {
                    Log.e(TAG, "No release found!")
                    return@withContext null
                }

                val assetName = releaseTargetDetails.getString("name")
                val downloadUrl = releaseTargetDetails.getString("browser_download_url")
                val downloadSize = releaseTargetDetails.getLong("size")
                val version = SemanticVersionNew.parse(tagName)
                return@withContext DownloadAsset(assetName, version, downloadUrl, downloadSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest release info: ${e.message}")
                return@withContext null
            }
        }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAppUpdate(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit
    ) {
        Log.d("$TAG-DL","URL: $downloadUrl, PATH: $fileName")
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Configure the request
        val request = DownloadManager.Request(downloadUrl.toUri()).apply {
            setTitle("Downloading Update")
            setDescription("Your app update is being downloaded...")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true) // Allow on metered networks
            setAllowedOverRoaming(true) // Allow on roaming
        }

        // Enqueue the download
        val downloadId = downloadManager.enqueue(request)

        CoroutineScope(Dispatchers.Main).launch {
            var isDownloading = true
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloaded =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (totalBytes > 0) {
                        val progress = (bytesDownloaded * 100L / totalBytes).toInt()
                        onProgress(DownloadProgress(fileName, progress))
                    }

                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                        Log.e("DownloadManager", "Download failed.")
                    }

                    cursor.close()
                }
                delay(500) // Poll every 500ms
            }
        }

        val onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                // Check if this is the download we're waiting for
                if (id == downloadId) {
                    val fileUri = downloadManager.getUriForDownloadedFile(downloadId)
                    launchInstaller(context, fileUri)
                    context.unregisterReceiver(this) // Unregister receiver after completion
                }
            }
        }

        // Register the receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                onCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun launchInstaller(context: Context, fileUri: Uri?) {
        if (fileUri == null) {
            Log.e("AppUpdate", "File URI is null. Cannot launch installer.")
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdate", "Error launching installer: ${e.message}")
        }
    }
}