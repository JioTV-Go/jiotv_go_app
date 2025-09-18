package com.skylake.skytv.jgorunner.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupDialog(
    showDialog: Boolean,
    context: Context,
    onDismiss: () -> Unit,
    onBackup: (File) -> Unit,
    onRestore: (File) -> Unit
) {
    var isLoginSelected by remember { mutableStateOf(false) }
    var isSettingsSelected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val file = convertUriToFile(context, uri)
            if (file != null) {
                restoreBackup(context, file)
                successMessage = "Restore successfully completed."
            }
        }
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Backup & Restore", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch between Backup and Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { showBackupOptionsDialog = true }) {
                            Text("Backup")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { restoreBackupLauncher.launch(arrayOf("application/zip")) }) {
                            Text("Restore")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Loading dialog
                    if (isLoading) {
                        CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    // Success message
                    if (successMessage.isNotEmpty()) {
                        Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Backup Options Dialog
    if (showBackupOptionsDialog) {
        Dialog(
            onDismissRequest = { showBackupOptionsDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Select Backup Options", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Login")
                        Checkbox(
                            checked = isLoginSelected,
                            onCheckedChange = { isLoginSelected = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Settings")
                        Checkbox(
                            checked = isSettingsSelected,
                            onCheckedChange = { isSettingsSelected = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Backup Button
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            if (isLoginSelected || isSettingsSelected) {
                                isLoading = true
                                // Handle backup logic
                                val backupFile = createBackupInExternalFolder(context, isLoginSelected, isSettingsSelected)
                                onBackup(backupFile)
                                successMessage = "Backup successfully saved."
                                isLoading = false
                            } else {
                                successMessage = "Please select at least one option."
                            }
                            showBackupOptionsDialog = false
                        }) {
                            Text("Start Backup")
                        }

                        Button(onClick = { showBackupOptionsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}



fun createBackupInExternalFolder(
    context: Context,
    isLoginSelected: Boolean,
    isSettingsSelected: Boolean
): File {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            100
        )
    }

    val externalBackupDir = File(
        Environment.getExternalStorageDirectory(),
        "backup_jtv"
    )

    if (!externalBackupDir.exists()) {
        val created = externalBackupDir.mkdirs()
        if (!created) {
            throw IllegalStateException("Failed to create external backup directory: $externalBackupDir")
        }
    }

    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val currentDateAndTime = dateFormat.format(Date())
    val backupFile = File(externalBackupDir, "shared_prefs_backup_$currentDateAndTime.zip")
    val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
    val loginDir = File(context.filesDir.parent, "files")

    ZipOutputStream(FileOutputStream(backupFile)).use { zipOutputStream ->
        // Backup login file if selected
        if (isLoginSelected) {

            val loginFile = File(loginDir, "store_v4.toml")
            if (loginFile.exists()) {
                FileInputStream(loginFile).use { inputStream ->
                    val entry = ZipEntry("store_v4.toml")
                    zipOutputStream.putNextEntry(entry)
                    inputStream.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        }

        // Backup settings file if selected
        if (isSettingsSelected) {
            val settingsFile = File(sharedPrefsDir, "SkySharedPref.xml")
            if (settingsFile.exists()) {
                FileInputStream(settingsFile).use { inputStream ->
                    val entry = ZipEntry("SkySharedPref.xml")
                    zipOutputStream.putNextEntry(entry)
                    inputStream.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        }
    }

    return backupFile
}


fun convertUriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "restored_backup.zip")
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun restoreBackup(context: Context, backupFile: File) {
    val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
    val loginDir = File(context.filesDir.parent, "files")
    sharedPrefsDir.mkdirs()

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            100
        )
    }

    ZipInputStream(FileInputStream(backupFile)).use { zipInputStream ->
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (entry.name == "store_v4.toml") {
                val outputFile = File(loginDir, entry.name)
                FileOutputStream(outputFile).use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            } else {
                val outputFile = File(sharedPrefsDir, entry.name)
                FileOutputStream(outputFile).use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

        }
    }
}
