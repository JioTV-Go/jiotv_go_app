package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skylake.skytv.jgorunner.utils.findActivity
import com.skylake.skytv.jgorunner.utils.getStoragePermissions
import com.skylake.skytv.jgorunner.utils.hasNotificationPermission
import com.skylake.skytv.jgorunner.utils.hasStoragePermission
import com.skylake.skytv.jgorunner.utils.isIgnoringBatteryOptimizations
import com.skylake.skytv.jgorunner.utils.isLeanbackTV
import com.skylake.skytv.jgorunner.utils.requestBatteryOptimizationExemption

@Composable
fun PermissionSetup(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onAllPermissionsReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTV = remember { context.isLeanbackTV() }

    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var batteryGranted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var storageGranted by remember { mutableStateOf(hasStoragePermission(context)) }

    
    var isNotificationPermanentlyDenied by remember { mutableStateOf(false) }

    fun syncPermissions() {
        notificationGranted = hasNotificationPermission(context)
        batteryGranted = isIgnoringBatteryOptimizations(context)
        storageGranted = hasStoragePermission(context)

        
        if (notificationGranted) isNotificationPermanentlyDenied = false

        if (notificationGranted && batteryGranted) {
            onAllPermissionsReady()
        }
    }

    
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        syncPermissions()
    }

    
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        syncPermissions()

        
        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val activity = context.findActivity()
            if (activity != null) {
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                
                if (!shouldShowRationale) {
                    isNotificationPermanentlyDenied = true
                }
            }
        }
    }

    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isRowLayout = isTV || isLandscape

    
    val notifDesc = when {
        notificationGranted -> "Allowed"
        isNotificationPermanentlyDenied -> "Open Settings to allow"
        else -> "Permission required"
    }

    val notifActionIcon = if (isNotificationPermanentlyDenied && !notificationGranted)
        Icons.Default.Settings else Icons.Default.ChevronRight

    if (isRowLayout) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            PermissionItemCard(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                actionIcon = notifActionIcon,
                granted = notificationGranted,
                description = notifDesc,
                isDark = isDark,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (isNotificationPermanentlyDenied) {
                            
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            settingsLauncher.launch(intent)
                        } else {
                            
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            PermissionItemCard(
                title = "Battery Optimization",
                icon = Icons.Default.BatterySaver,
                actionIcon = Icons.Default.ChevronRight,
                granted = batteryGranted,
                description = if (batteryGranted) "Exempted from battery saver" else "Needs exemption",
                isDark = isDark,
                onClick = { requestBatteryOptimizationExemption(context) },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            PermissionItemCard(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                actionIcon = notifActionIcon,
                granted = notificationGranted,
                description = notifDesc,
                isDark = isDark,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (isNotificationPermanentlyDenied) {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            settingsLauncher.launch(intent)
                        } else {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            )

            PermissionItemCard(
                title = "Battery Optimization",
                icon = Icons.Default.BatterySaver,
                actionIcon = Icons.Default.ChevronRight,
                granted = batteryGranted,
                description = if (batteryGranted) "Exempted from battery saver" else "Needs exemption",
                isDark = isDark,
                onClick = { requestBatteryOptimizationExemption(context) }
            )
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    icon: ImageVector,
    actionIcon: ImageVector,
    granted: Boolean,
    description: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused && isDark -> Color(0xFF26264A)
        isFocused && !isDark -> Color(0xFFE4E4FF)
        isDark -> Color(0xFF151529)
        else -> Color(0xFFF3F3FF)
    }

    val labelColor = if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val statusColor = when {
        granted && isDark -> Color(0xFF77FFAA)
        granted && !isDark -> Color(0xFF1B8746)
        !granted && isDark -> Color(0xFFFF7777)
        else -> Color(0xFFD22B2B)
    }

    val border = if (isFocused) {
        BorderStroke(2.dp, labelColor)
    } else null

    Card(
        shape = RoundedCornerShape(20.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .heightIn(min = 80.dp)
            .shadow(elevation = if (isFocused) 8.dp else 4.dp, shape = RoundedCornerShape(20.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !granted
            ) {
                onClick()
            }
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
                    .size(28.dp)
                    .padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else actionIcon,
                contentDescription = if (granted) "Granted" else "Action needed",
                tint = if (granted) statusColor else labelColor.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}