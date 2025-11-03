package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.core.update.BinaryUpdater
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.utils.CustButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BinarySetup(
    preferenceManager: SkySharedPref,
    isDark: Boolean,
    onCompleteStep: () -> Unit
) {
    val context = LocalContext.current

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Fetching latest binary info...") }
    var completed by remember { mutableStateOf(false) }
    var asset by remember {
        mutableStateOf<com.skylake.skytv.jgorunner.core.update.DownloadAsset?>(
            null
        )
    }
    var binaryInstalled by remember { mutableStateOf(false) }

    val accent = if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val subText = if (isDark) Color(0xFF8E90D9) else Color(0xFF6D6FF5)
    val isRed = if (isDark) Color(0xFFFF7777) else Color(0xFFD22B2B)
    val isGreen = if (isDark) Color(0xFF77FFAA) else Color(0xFF22BB66)

    LaunchedEffect(Unit) {
        val release = BinaryUpdater.fetchLatestReleaseInfo()
        if (release == null) {
            statusText = "Failed to fetch release info"
            return@LaunchedEffect
        }

        asset = release
        val localFile = File(context.filesDir, release.name)
        binaryInstalled = localFile.exists()

        if (binaryInstalled) {
            statusText = "Binary already installed"
        } else {
            statusText = "Starting automatic download..."
            isDownloading = true

            performBinaryUpdate(
                context = context,
                preferenceManager = preferenceManager,
                asset = release,
                onProgress = {
                    progress = it
                    statusText = "Downloading... ${(it * 100).toInt()}%"
                },
                onSuccess = {
                    statusText = "Download complete ✅"
                    isDownloading = false
                    completed = true
                    binaryInstalled = localFile.exists()
                },
                onError = {
                    statusText = "Failed: $it"
                    isDownloading = false
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JioTV GO ${asset?.version ?: ""}",
            color = accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))
        if (asset != null) {
            Text(
                text = when {
                    isDownloading -> "Downloading..."
                    binaryInstalled -> "Binary installed"
                    else -> "Binary not installed"
                },
                color = when {
                    isDownloading -> subText
                    binaryInstalled -> isGreen
                    else -> isRed
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isDownloading -> {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%", color = accent, fontSize = 16.sp)
            }

            completed -> {
                preferenceManager.myPrefs.jtvGoBinaryVersion =
                    asset?.version.toString()
                preferenceManager.myPrefs.jtvGoBinaryName = asset?.name
                preferenceManager.savePreferences()
                CustButton(
                    onClick = { onCompleteStep() },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Continue", color = Color(0xFF151529))
                }
            }

            else -> {
                Text(
                    text = statusText,
                    color = subText,
                    fontSize = 14.sp
                )
            }
        }
    }
}


fun performBinaryUpdate(
    context: Context,
    preferenceManager: SkySharedPref,
    asset: com.skylake.skytv.jgorunner.core.update.DownloadAsset,
    onProgress: (Float) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = asset.downloadUrl
            val fileName = asset.name
            val output = File(context.filesDir, fileName)

            val connection = URL(url).openConnection()
            val total = connection.contentLength

            connection.getInputStream().use { input ->
                output.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        out.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) {
                            onProgress(downloaded.toFloat() / total)
                        }
                        bytes = input.read(buffer)
                    }
                }
            }

            preferenceManager.myPrefs.jtvGoBinaryName = fileName
            preferenceManager.savePreferences()

            withContext(Dispatchers.Main) {
                onSuccess()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Error downloading binary")
            }
        }
    }
}