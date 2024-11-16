package com.skylake.skytv.jgorunner.core.execution

import android.content.Context
import android.os.Build
import android.util.Log
import com.skylake.skytv.jgorunner.data.SkySharedPref
import java.io.File
import java.io.InputStream

object BinaryExecutor {
    private var binaryProcess: Process? = null
    private val outputBuilder = StringBuilder()
    private const val TAG = "BinaryExecutor"

    fun executeBinary(
        context: Context,
        binaryFile: File,
        arguments: Array<String>?,
        onOutput: (String) -> Unit,
        onError: (ExecutionError) -> Unit
    ) {
        Thread {
            try {
                if (!binaryFile.exists()) {
                    onError(
                        ExecutionError(
                            ExecutionError.ExecutionErrorType.BINARY_NOT_FOUND,
                            "Binary file not found"
                        )
                    )
                    return@Thread
                }

                setBinaryExecutable(binaryFile)
                val args = buildArgsList(context, arguments)
                Log.d(TAG, "Executing binary: $args")
                binaryProcess = Runtime.getRuntime().exec(
                    arrayOf(
                        "sh",
                        "-c",
                        arrayOf(
                            binaryFile.absolutePath,
                            *args.toTypedArray()
                        ).joinToString(" ")
                    ),
                    null, // Environment variables (null means inherit from parent process)
                    binaryFile.parentFile // Set the working directory
                )

                Thread(
                    StreamGobbler(
                        binaryProcess!!.inputStream,
                        onOutput
                    )
                ).start()
                Thread(
                    ErrorStreamGobbler(
                        binaryProcess!!.errorStream,
                        onOutput
                    )
                ).start()

                Log.d(TAG, "Binary started successfully with PID: ${getProcessId(binaryProcess!!)}")
                binaryProcess!!.waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing binary: ", e)
                onError(
                    ExecutionError(
                        ExecutionError.ExecutionErrorType.BINARY_UNKNOWN_ERROR,
                        e.message ?: "Unknown error",
                        e
                    )
                )
            }
        }.start()
    }

    private fun setBinaryExecutable(binaryFile: File) {
        if (binaryFile.exists()) {
            val success = binaryFile.setExecutable(true, false)
            // Intentionally ignore the result without further action
            Log.d(TAG, "binaryPermissions: $success")
        }
    }

    private fun buildArgsList(
        context: Context,
        arguments: Array<String>?
    ): List<String> {
        if (!arguments.isNullOrEmpty()) {
            return arguments.toList()
        }

        val port = SkySharedPref.getInstance(context).myPrefs.jtvGoServerPort
        val jtvConfigLocation = SkySharedPref.getInstance(context).myPrefs.jtvConfigLocation

        val command = mutableListOf(
            "run",
        )
        if (port in 1..65535) {
            command.add("--port")
            command.add(port.toString())
        }
        if (!SkySharedPref.getInstance(context).myPrefs.serveLocal)
            command.add("--public")

        command.add("--config")
        command.add("\"${jtvConfigLocation}\"")

        return command
    }

    fun stopBinary() {
        if (binaryProcess != null) {
            try {
                // Close all streams before killing the process
                closeProcessStreams(binaryProcess!!)

                // Retrieve the process ID (PID)
                val pid = getProcessId(binaryProcess!!)
                if (pid != null) {
                    Log.i(TAG, "Killing process tree for PID: $pid")

                    // Execute shell command to kill the entire process tree
                    killProcessTree(pid)
                }

                // First attempt to stop the process gracefully
                binaryProcess!!.destroy()
                Log.i(TAG + "DIX", "Process destroyed.")

                // If needed, escalate to forcibly destroying it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binaryProcess!!.destroyForcibly()
                }
                Log.i(TAG, "Process forcibly destroyed.")
            } catch (e: Exception) {
                Log.e(TAG, "Error while stopping binary process: ", e)
            } finally {
                binaryProcess = null // Reset the process variable
            }
        }
    }

    // Helper method to retrieve the process ID (PID) via reflection
    private fun getProcessId(process: Process): String? {
        try {
            // Use reflection to access the PID field in the Process class
            val pidField = process.javaClass.getDeclaredField("pid")
            pidField.isAccessible = true
            return pidField.getInt(process).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting process ID: ", e)
            return null
        }
    }

    // Helper method to kill the process tree using a shell command
    private fun killProcessTree(pid: String) {
        try {
            // Execute shell command to kill the process tree (including subprocesses)
            val cmd = arrayOf(
                "sh", "-c",
                "pkill -P $pid"
            )
            val killProcess = Runtime.getRuntime().exec(cmd)
            killProcess.waitFor() // Wait for the kill command to finish
            Log.i(TAG, "Successfully killed process tree for PID: $pid")
        } catch (e: Exception) {
            Log.e(TAG, "Error killing process tree: ", e)
        }
    }

    // Helper method to close process streams
    private fun closeProcessStreams(process: Process) {
        try {
            process.inputStream.close()
            process.outputStream.close()
            process.errorStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing process streams: ", e)
        }
    }

    private class StreamGobbler(
        private val inputStream: InputStream,
        private val onOutput: (String) -> Unit
    ) : Runnable {
        override fun run() {
            try {
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                    val output = String(buffer, 0, bytesRead)
                    outputBuilder.append(output).append("\n")
                    Log.d(TAG, "Process output: $output")
                    onOutput(output)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading binary output: ", e)
            }
        }
    }

    private class ErrorStreamGobbler(
        private val errorStream: InputStream,
        private val onError: (String) -> Unit
    ) : Runnable {
        override fun run() {
            try {
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while ((errorStream.read(buffer).also { bytesRead = it }) != -1) {
                    val output = String(buffer, 0, bytesRead)
                    Log.e(TAG, "Process error: $output")
                    onError(output)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading binary error output: ", e)
            }
        }
    }
}

