package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

@SuppressLint("UseCompatLoadingForDrawables")
fun getDrawableOrFallback(context: Context, resId: Int, fallbackResId: Int): Drawable {
    return try {
        context.getDrawable(resId) ?: context.getDrawable(fallbackResId)!!
    } catch (e: Exception) {
        context.getDrawable(fallbackResId)!!
    }
}

@Composable
fun AppListScreen(modifier: Modifier = Modifier, onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val apps = remember { mutableStateListOf<AppInfo>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            getInstalledApps(context).collect { app ->
                apps.add(app)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
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

@SuppressLint("UseCompatLoadingForDrawables")
fun getInstalledApps(context: Context): Flow<AppInfo> = flow {
    val packageManager: PackageManager = context.packageManager
    val fallbackIconResId = R.mipmap.ic_launcher_neo // Ensure this resource always exists

    val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { appInfo -> appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
        .sortedBy { appInfo -> packageManager.getApplicationLabel(appInfo).toString().lowercase() }

    // Add "No IPTV" option
    emit(AppInfo(
        appName = "No IPTV",
        icon = getDrawableOrFallback(context, R.drawable.cancel_24px, fallbackIconResId),
        packageName = "",
        launchActivity = ""
    ))

    // Add "New TV UI" option
    emit(AppInfo(
        appName = "New TV UI",
        icon = getDrawableOrFallback(context, R.mipmap.ic_launcher_neodark, fallbackIconResId),
        packageName = "tvzone",
        launchActivity = ""
    ))

    // Add "WEB TV" option
    emit(AppInfo(
        appName = "WEB TV - {browser based}",
        icon = getDrawableOrFallback(context, R.mipmap.ic_launcher_neo, fallbackIconResId),
        packageName = "webtv",
        launchActivity = ""
    ))

    // Add "Sonata - {ALPHA}" option
    emit(AppInfo(
        appName = "Sonata - {ALPHA}",
        icon = getDrawableOrFallback(context, R.drawable.exo_loading_blue, fallbackIconResId),
        packageName = "sonata",
        launchActivity = ""
    ))

    
    for (appInfo in apps) {
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val appIcon = try {
            packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: Exception) {
            context.getDrawable(fallbackIconResId)!!
        }
        val packageName = appInfo.packageName
        val launchActivity =
            packageManager.getLaunchIntentForPackage(packageName)?.component?.className ?: ""
        emit(AppInfo(appName, appIcon, packageName, launchActivity))
    }
}.flowOn(Dispatchers.IO)
