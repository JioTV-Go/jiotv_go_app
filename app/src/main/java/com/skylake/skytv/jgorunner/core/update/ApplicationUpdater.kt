package com.skylake.skytv.jgorunner.core.update

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApplicationUpdater {
    private const val TAG = "JTVGo::AppUpdater"

    // Action used for PackageInstaller session status callbacks.
    private const val INSTALL_STATUS_ACTION =
        "com.skylake.skytv.jgorunner.action.INSTALL_STATUS"

    @Volatile
    private var installReceiverRegistered = false

    @Volatile
    var onInstallFailed: (() -> Unit)? = null

    /**
     * Fetches the newest release asset for THIS build's edition — the normal update path.
     */
    suspend fun fetchLatestReleaseInfo(): DownloadAsset? =
        fetchReleaseAssetForEdition(wantLite = false)

    /**
     * Fetches the newest release asset for a specific edition, regardless of which edition
     * is currently installed. Used by the in-app edition switcher; [fetchLatestReleaseInfo]
     * delegates here with the running edition.
     *
     * @param wantLite true for the lite APK, false for the full APK.
     */
    suspend fun fetchReleaseAssetForEdition(wantLite: Boolean): DownloadAsset? =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://api.github.com/repos/JioTV-Go/jiotv_go_app/releases/latest"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")

                if (assets.length() == 0) {
                    Log.e(TAG, "No release assets found!")
                    return@withContext null
                }

                // Pick the best matching APK instead of blindly taking asset[0].
                val abis = Build.SUPPORTED_ABIS?.map { it.lowercase() } ?: emptyList()
                var byAbi: JSONObject? = null
                var universal: JSONObject? = null
                var anyApk: JSONObject? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name").lowercase()
                    if (!name.endsWith(".apk")) continue
                    if (name.contains("lite") != wantLite) continue
                    if (anyApk == null) anyApk = asset
                    if (universal == null && name.contains("universal")) universal = asset
                    if (byAbi == null && abis.any { abi -> name.contains(abi) }) byAbi = asset
                }

                val chosen = byAbi ?: universal ?: anyApk ?: run {
                    Log.e(TAG, "No release asset for edition (wantLite=$wantLite)")
                    return@withContext null
                }

                val assetName = chosen.getString("name")
                val downloadUrl = chosen.getString("browser_download_url")
                val downloadSize = chosen.optLong("size", 0L)
                val version = SemanticVersionNew.parseOrDefault(tagName)
                Log.d(TAG, "Chosen asset: $assetName ($downloadUrl), abis=$abis")
                return@withContext DownloadAsset(assetName, version, downloadUrl, downloadSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest release info: ${e.message}", e)
                return@withContext null
            }
        }

    /**
     * Downloads the update APK directly to the app's own storage (no DownloadManager,
     * no public Downloads dir, no FileProvider) and then installs it via PackageInstaller.
     */
    fun downloadAppUpdate(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val appContext = context.applicationContext
        Log.d("$TAG-DL", "Starting download. URL: $downloadUrl, file: $fileName")

        // Need "install unknown apps" before we bother downloading.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            requestInstallPermission(appContext)
            CoroutineScope(Dispatchers.Main).launch {
                onInstallFailed?.invoke()
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val safeName = if (fileName.endsWith(".apk")) fileName else "update.apk"
            val outFile = File(appContext.cacheDir, safeName)
            try {
                val ok = downloadToFile(downloadUrl, outFile) { progress ->
                    onProgress(DownloadProgress(safeName, progress))
                }
                if (!ok || !outFile.exists() || outFile.length() == 0L) {
                    Log.e(TAG, "Download failed or produced empty file.")
                    toast(appContext, "Update download failed")
                    onProgress(DownloadProgress(safeName, -1))
                    return@launch
                }
                Log.d("$TAG-DL", "Download complete (${outFile.length()} bytes). Installing.")
                installApk(appContext, outFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update: ${e.message}", e)
                toast(appContext, "Update error: ${e.message}")
                onProgress(DownloadProgress(safeName, -1))
            }
        }
    }

    private fun downloadToFile(
        urlString: String,
        outFile: File,
        onProgress: (Int) -> Unit
    ): Boolean {
        var connection: HttpURLConnection? = null
        var currentUrl = urlString
        var redirects = 0
        try {
            while (true) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/octet-stream")
                }
                val code = connection.responseCode
                if (code in intArrayOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308
                    )
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location == null || ++redirects > 5) {
                        Log.e(TAG, "Too many redirects or missing Location header.")
                        return false
                    }
                    currentUrl = location
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Download HTTP error: $code")
                    return false
                }
                break
            }

            val total = connection!!.contentLength.toLong()
            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var read: Int
                    var lastPct = -1
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = (downloaded * 100L / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
            onProgress(100)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "downloadToFile error: ${e.message}", e)
            return false
        } finally {
            connection?.disconnect()
        }
    }

    private fun installApk(appContext: Context, apkFile: File) {
        registerInstallReceiver(appContext)
        var session: PackageInstaller.Session? = null
        try {
            val installer = appContext.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = installer.createSession(params)
            session = installer.openSession(sessionId)

            apkFile.inputStream().use { input ->
                session.openWrite("update.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val statusIntent = Intent(INSTALL_STATUS_ACTION).setPackage(appContext.packageName)
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext, sessionId, statusIntent, piFlags
            )

            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "PackageInstaller session committed (id=$sessionId).")
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller install failed: ${e.message}", e)
            try { session?.abandon() } catch (_: Exception) {}
            if (!installViaViewIntent(appContext, apkFile)) {
                toast(appContext, "Install error: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    onInstallFailed?.invoke()
                }
            }
        } finally {
            try { session?.close() } catch (_: Exception) {}
        }
    }

    private fun installViaViewIntent(appContext: Context, apkFile: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                appContext, "${appContext.packageName}.provider", apkFile
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (viewIntent.resolveActivity(appContext.packageManager) == null) {
                Log.w(TAG, "Fallback installer: no activity handles ACTION_VIEW for APK.")
                return false
            }
            appContext.startActivity(viewIntent)
            Log.d(TAG, "Fallback installer launched via ACTION_VIEW.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback installer failed: ${e.message}", e)
            false
        }
    }

    private fun requestInstallPermission(appContext: Context) {
        toast(appContext, "Allow installing apps from this source, then press Update again")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${appContext.packageName}".toUri()
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            appContext.startActivity(settingsIntent)
            Log.w(TAG, "Unknown sources permission missing. Opened per-app settings.")
        } catch (e: Exception) {
            Log.w(TAG, "Per-app unknown-sources screen unavailable: ${e.message}")
            try {
                val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                appContext.startActivity(fallback)
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open any security settings: ${e2.message}", e2)
                toast(
                    appContext,
                    "Enable Unknown sources in Settings > Security, then press Update again"
                )
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerInstallReceiver(appContext: Context) {
        if (installReceiverRegistered) return
        synchronized(this) {
            if (installReceiverRegistered) return
            val filter = IntentFilter(INSTALL_STATUS_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(
                    installStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                appContext.registerReceiver(installStatusReceiver, filter)
            }
            installReceiverRegistered = true
        }
    }

    private val installStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != INSTALL_STATUS_ACTION) return
            val appContext = context.applicationContext
            val status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    @Suppress("DEPRECATION")
                    val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    if (confirmIntent == null) {
                        Log.e(TAG, "Pending user action but no confirm intent provided.")
                        toast(appContext, "Install confirmation unavailable on this device")
                        return
                    }
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        appContext.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch install confirm dialog: ${e.message}", e)
                        toast(appContext, "Could not open install screen")
                    }
                }

                PackageInstaller.STATUS_SUCCESS -> {
                    Log.d(TAG, "Update installed successfully. Relaunching app.")
                    relaunchApp(appContext)
                }

                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e(TAG, "Install failed (status=$status): $msg")
                    toast(appContext, "Update install failed: $msg")
                    CoroutineScope(Dispatchers.Main).launch {
                        onInstallFailed?.invoke()
                    }
                }
            }
        }
    }

    private fun relaunchApp(appContext: Context) {
        try {
            val launchIntent = appContext.packageManager
                .getLaunchIntentForPackage(appContext.packageName)
            if (launchIntent == null) {
                Log.w(TAG, "No launch intent found; cannot relaunch.")
                return
            }
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            appContext.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to relaunch app after update: ${e.message}", e)
        }
    }

    private fun toast(appContext: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
        }
    }
}