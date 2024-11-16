package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.skylake.skytv.jgorunner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch


@Composable
fun AppListScreen(modifier: Modifier = Modifier, onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val apps = remember { mutableStateListOf<AppInfo>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            getInstalledApps(context).collect {
                apps.add(it)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        items(apps) { app ->
            AppListItem(appInfo = app, onAppSelected = onAppSelected)
            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        }
    }
}

@Composable
fun AppListItem(appInfo: AppInfo, onAppSelected: (AppInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onAppSelected(appInfo) }
    ) {
        val drawable = appInfo.icon
        val bitmap = drawable.toBitmap().asImageBitmap()

        Image(
            bitmap = bitmap,
            contentDescription = "App Icon",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}


data class AppInfo(
    val appName: String,
    val icon: Drawable,
    val packageName: String,
    val launchActivity: String
)

fun getInstalledApps(context: Context): Flow<AppInfo> = flow {
    val packageManager: PackageManager = context.packageManager

    // Get installed applications
    val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { appInfo ->
            // Filter out system apps
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
        }
        .sortedBy { appInfo ->
            // Sort by app name
            packageManager.getApplicationLabel(appInfo).toString().lowercase()
        }

    // Add a none action
    val noneOption = AppInfo(
        appName = "No IPTV",
        icon = context.getDrawable(R.drawable.cancel_24px)!!,
        packageName = "",
        launchActivity = ""
    )
    emit(noneOption)

    // Add WebTV action
    val webOption = AppInfo(
        appName = "WEB TV",
        icon = context.getDrawable(R.mipmap.ic_launcher_neo)!!,
        packageName = "webtv",
        launchActivity = ""
    )
    emit(webOption)

    for (appInfo in apps) {
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val appIcon = packageManager.getApplicationIcon(appInfo)
        val packageName = appInfo.packageName
        val launchActivity =
            packageManager.getLaunchIntentForPackage(packageName)?.component?.className
                ?: ""
        emit(AppInfo(appName, appIcon, packageName, launchActivity))
    }
}.flowOn(Dispatchers.IO)