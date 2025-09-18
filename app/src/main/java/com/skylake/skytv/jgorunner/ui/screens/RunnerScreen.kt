package com.skylake.skytv.jgorunner.ui.screens
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.core.execution.BinaryExecutor
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File
import java.io.FileOutputStream

@Composable
fun RunnerScreen(context: Context) {
    var logOutput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    var selectedFilePath by remember { mutableStateOf("") }
    val preferenceManager = SkySharedPref.getInstance(context)
    val focusRequester = remember { FocusRequester() }
    var isChecked by remember { mutableStateOf(preferenceManager.myPrefs.expDebug) }
    var isCheckedepg by remember { mutableStateOf(preferenceManager.myPrefs.epgDebug) }

    // File picker launcher to allow user to select a file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val file = File(context.filesDir, "majorbin")
                val success = copyFileToInternalStorage(context, uri, file)
                if (success) {
                    selectedFilePath = file.absolutePath
                    Toast.makeText(context, "Binary replaced successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to replace binary", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "### JTV-GO Server ###",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "### Limited Test Version ###",
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to select a custom binary file
        Button(
            onClick = {
                filePickerLauncher.launch("*/*") // Launch the file picker to select binary
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Select Custom Binary") // Renamed button
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input box for custom command
        TextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            label = { Text("Enter Command") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                showDialog = true // Show dialog for command input
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Run Binary Command")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Experimental Playlist Support")
            Switch(
                checked = isChecked,
                onCheckedChange = { newCheckedState ->
                    isChecked = newCheckedState
                    preferenceManager.myPrefs.expDebug = newCheckedState
                    val status = if (newCheckedState) "enabled" else "disabled"
                    Toast.makeText(context, "Experimental features $status", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Experimental EPG")
            Switch(
                checked = isCheckedepg,
                onCheckedChange = { newCheckedState ->
                    isCheckedepg = newCheckedState
                    preferenceManager.myPrefs.epgDebug = newCheckedState
                    val status = if (newCheckedState) "enabled" else "disabled"
                    Toast.makeText(context, "Experimental EPG: $status", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Output log
        Text(text = logOutput, fontSize = 14.sp, modifier = Modifier.padding(16.dp))

        if (showDialog) {
            CommandInputDialog(
                onDismiss = { showDialog = false },
                onRunCommand = { command ->
                    val binaryFile = File(context.filesDir, "majorbin")
                    BinaryExecutor.executeBinary(
                        context,
                        binaryFile,
                        command.split(" ").toTypedArray(),
                        { output -> logOutput += output + "\n" },
                        { error -> logOutput += "Error: ${error.message}\n" }
                    )
                    showDialog = false
                },
                commandInput = commandInput,
                onCommandInputChange = { commandInput = it }
            )
        }
    }
}

@Composable
fun CommandInputDialog(
    onDismiss: () -> Unit,
    onRunCommand: (String) -> Unit,
    commandInput: String,
    onCommandInputChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Input Command") },
        text = {
            TextField(
                value = commandInput,
                onValueChange = onCommandInputChange,
                label = { Text("Command") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onRunCommand(commandInput) // Run the command
                }
            ) {
                Text("Run")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Function to copy selected file to internal storage
fun copyFileToInternalStorage(context: Context, uri: Uri, destFile: File): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true // Successfully copied
    } catch (e: Exception) {
        Log.e("FileCopy", "Error copying file: ${e.message}")
        false // Failed to copy
    }
}