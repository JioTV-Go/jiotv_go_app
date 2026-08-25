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
import kotlinx.coroutines.flow.toList

@SuppressLint("UseCompatLoadingForDrawables")
fun getDrawableOrFallback(context: Context, resId: Int, fallbackResId: Int): Drawable {
    return runCatching {
        context.getDrawable(resId) ?: context.getDrawable(fallbackResId)!!
    }.getOrElse {
        context.getDrawable(fallbackResId)!!
    }
}

@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    onAppSelected: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val apps by produceState(initialValue = emptyList<AppInfo>(), context) {
        value = getInstalledApps(context).toList()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(apps) { app ->
            AppListItem(appInfo = app, onAppSelected = onAppSelected)
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun AppListItem(appInfo: AppInfo, onAppSelected: (AppInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppSelected(appInfo) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = appInfo.icon.toBitmap().asImageBitmap(),
            contentDescription = "App Icon",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = appInfo.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    val packageManager = context.packageManager
    val fallbackIconResId = R.mipmap.ic_launcher_neo

    // Predefined shortcuts
    listOf(
        AppInfo(
            appName = "No IPTV",
            icon = getDrawableOrFallback(context, R.drawable.cancel_24px, fallbackIconResId),
            packageName = "",
            launchActivity = ""
        ),
        AppInfo(
            appName = "New TV UI",
            icon = getDrawableOrFallback(context, R.mipmap.ic_launcher_neodark, fallbackIconResId),
            packageName = "tvzone",
            launchActivity = ""
        ),
        AppInfo(
            appName = "WEB TV - {browser based}",
            icon = getDrawableOrFallback(context, R.mipmap.ic_launcher_neo, fallbackIconResId),
            packageName = "webtv",
            launchActivity = ""
        )
    ).forEach { emit(it) }

    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
        .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }

    for (appInfo in installedApps) {
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val appIcon = runCatching {
            packageManager.getApplicationIcon(appInfo.packageName)
        }.getOrElse {
            context.getDrawable(fallbackIconResId)!!
        }
        val packageName = appInfo.packageName
        val launchActivity =
            packageManager.getLaunchIntentForPackage(packageName)?.component?.className.orEmpty()

        emit(AppInfo(appName, appIcon, packageName, launchActivity))
    }
}.flowOn(Dispatchers.IO)
