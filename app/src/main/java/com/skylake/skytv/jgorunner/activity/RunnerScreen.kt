package com.skylake.skytv.jgorunner.activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.*

@Composable
fun RunnerScreen(context: Context) {
    val preferenceManager = remember { SkySharedPref(context) }
    var logOutput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    var selectedFilePath by remember { mutableStateOf("") }

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

        // Output log
        Text(text = logOutput, fontSize = 14.sp, modifier = Modifier.padding(16.dp))

        if (showDialog) {
            CommandInputDialog(
                onDismiss = { showDialog = false },
                onRunCommand = { command ->
                    val binaryFile = File(context.filesDir, "majorbin")
                    val output = runBinaryCommand(binaryFile.absolutePath, command, context)
                    logOutput += output + "\n"
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

// Function to run a binary command with arguments
fun runBinaryCommand(binaryPath: String, commandArgs: String, context: Context): String {
    return try {
        val command = "$binaryPath $commandArgs"
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        reader.close()
        process.waitFor()
        Log.d("BinaryCommand", output.toString())
        output.toString()
    } catch (e: Exception) {
        Log.e("BinaryCommand", "Error executing command: ${e.message}")
        "Error: ${e.message}"
    }
}
