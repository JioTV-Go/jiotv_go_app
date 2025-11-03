package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Icon
import com.skylake.skytv.jgorunner.activities.setup_wizard.SetupWizardActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.utils.findActivity
import com.skylake.skytv.jgorunner.utils.hasNotificationPermission
import com.skylake.skytv.jgorunner.utils.hasStoragePermission
import com.skylake.skytv.jgorunner.utils.isIgnoringBatteryOptimizations
import com.skylake.skytv.jgorunner.utils.requestBatteryOptimizationExemption
import com.skylake.skytv.jgorunner.utils.requestStoragePermission

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun PermissionSetup(
    isDark: Boolean,
    context: Context = LocalContext.current
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val isTV = context.packageManager.hasSystemFeature("android.software.leanback")

    if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)

    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var batteryGranted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var storageGranted by remember { mutableStateOf(hasStoragePermission(context)) }
    var isRequestInProgress by remember { mutableStateOf(false) }

    val orientation = context.resources.configuration.orientation
    val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = hasNotificationPermission(context)
                batteryGranted = isIgnoringBatteryOptimizations(context)
                storageGranted = hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refreshPermissions() {
        Handler(Looper.getMainLooper()).postDelayed({
            notificationGranted = hasNotificationPermission(context)
            batteryGranted = isIgnoringBatteryOptimizations(context)
            storageGranted = hasStoragePermission(context)
            isRequestInProgress = false
        }, 1200)
    }

    fun safeRequest(action: () -> Unit) {
        if (!isRequestInProgress) {
            isRequestInProgress = true
            action()
            refreshPermissions()
        }
    }

    val layoutModifier = Modifier.fillMaxWidth()
    val isRowLayout = isTV || isLandscape

    if (isRowLayout) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = layoutModifier
        ) {
            PermissionItemCard(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                granted = notificationGranted,
                description = if (notificationGranted) "Allowed" else "Permission required",
                isDark = isDark,
                isBusy = isRequestInProgress,
                onClick = { safeRequest { (activity as? SetupWizardActivity)?.askNotificationPermission() } },
                modifier = Modifier.weight(1f)
            )

            PermissionItemCard(
                title = "Battery Optimization",
                icon = Icons.Default.BatterySaver,
                granted = (batteryGranted || isTV),
                description = if (batteryGranted || isTV) "Exempted from battery saver" else "Needs exemption",
                isDark = isDark,
                isBusy = isRequestInProgress,
                onClick = { safeRequest { requestBatteryOptimizationExemption(context) } },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = layoutModifier) {
            PermissionItemCard(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                granted = notificationGranted,
                description = if (notificationGranted) "Allowed" else "Permission required",
                isDark = isDark,
                isBusy = isRequestInProgress
            ) {
                safeRequest { (activity as? SetupWizardActivity)?.askNotificationPermission() }
            }

            PermissionItemCard(
                title = "Battery Optimization",
                icon = Icons.Default.BatterySaver,
                granted = batteryGranted,
                description = if (batteryGranted) "Exempted from battery saver" else "Needs exemption",
                isDark = isDark,
                isBusy = isRequestInProgress
            ) {
                safeRequest { requestBatteryOptimizationExemption(context) }
            }

            if (false) {
                PermissionItemCard(
                    title = "File Storage Access",
                    icon = Icons.Default.Storage,
                    granted = storageGranted,
                    description = if (storageGranted) "Access granted" else "Permission required",
                    isDark = isDark,
                    isBusy = isRequestInProgress
                ) {
                    safeRequest { requestStoragePermission(activity) }
                }
            }

        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    icon: ImageVector,
    granted: Boolean,
    description: String,
    isDark: Boolean,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF151529) else Color(0xFFF3F3FF)
    val labelColor = if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val statusColor = when {
        granted && isDark -> Color(0xFF77FFAA)
        granted && !isDark -> Color(0xFF22BB66)
        !granted && isDark -> Color(0xFFFF7777)
        else -> Color(0xFFD22B2B)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .heightIn(min = 80.dp)
            .clickable(enabled = !isBusy) { onClick() }
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = labelColor,
                modifier = Modifier
                    .size(30.dp)
                    .padding(end = 14.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBusy) "Please wait..." else description,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = labelColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

