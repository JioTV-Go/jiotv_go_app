package com.skylake.skytv.jgorunner.core.update

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ketch.DownloadConfig
import com.ketch.DownloadModel
import com.ketch.Ketch
import com.ketch.NotificationConfig
import com.skylake.skytv.jgorunner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.cthing.versionparser.semver.SemanticVersion
import java.util.concurrent.TimeUnit

fun downloadFile(
    activity: ComponentActivity,
    url: String,
    fileName: String,
    path: String = activity.filesDir.absolutePath,
    onDownloadStatusUpdate: (DownloadModel) -> Unit
) {
    val ketch = Ketch.builder()
        .setOkHttpClient(
            okHttpClient = OkHttpClient
                .Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        ).setDownloadConfig(
        config = DownloadConfig(
            connectTimeOutInMs = 20000L,
            readTimeOutInMs = 15000L
        )
    ).setNotificationConfig(
        config = NotificationConfig(
            enabled = true, // Default: false
            smallIcon = R.drawable.notifications_24px,
        )
    ).build(activity)
    val id = ketch.download(url, path, fileName)
    activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            ketch.observeDownloadById(id)
                .flowOn(Dispatchers.IO)
                .collect { downloadModel ->
                    onDownloadStatusUpdate(downloadModel)
                }
        }
    }
}

data class DownloadAsset(
    val name: String,
    val version: SemanticVersion,
    val downloadUrl: String,
    val downloadSize: Long
)
