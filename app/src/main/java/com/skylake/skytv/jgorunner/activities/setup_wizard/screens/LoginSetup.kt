package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.skylake.skytv.jgorunner.core.update.BinaryUpdater
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.BinaryService
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

@Composable
fun LoginSetup(
    preferenceManager: SkySharedPref,
    isDark: Boolean,
    onCompleteStep: () -> Unit
) {
    val context = LocalContext.current
    var binaryInstalled by remember { mutableStateOf(false) }
    val isRed = if (isDark) Color(0xFFFF7777) else Color(0xFFD22B2B)

    LaunchedEffect(Unit) {
        val release = BinaryUpdater.fetchLatestReleaseInfo()
        if (release == null) {
            binaryInstalled = false
        } else {
            val localFile = File(context.filesDir, release.name)
            binaryInstalled = localFile.exists()
        }
    }

    LaunchedEffect(Unit) {
        try {
            Toast.makeText(context, "[JGO] Running Server in Background", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, BinaryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        } catch (e: Exception) {
            Log.e("LoginSetting", "Failed to start BinaryService", e)
        }
    }

    var phoneNumber by remember { mutableStateOf(TextFieldValue()) }
    var otpCode by remember { mutableStateOf(TextFieldValue()) }


    var isLoadingOtpSend by remember { mutableStateOf(false) }
    var isLoadingOtpVerify by remember { mutableStateOf(false) }

    val focusRequesterPhone = remember { FocusRequester() }
    val focusRequesterOtp = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val baseURL = "http://localhost:${preferenceManager.myPrefs.jtvGoServerPort}"

    var uiMessage by remember { mutableStateOf<String?>("Checking server status...") }
    var isServerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(baseURL) {
        var firstRun = true
        var stopUpdatingMessage = false
        while (true) {
            if (firstRun) {
                uiMessage = "Checking server status..."
                firstRun = false
            }
            checkServerStatus("$baseURL/play/143") { msg, running ->
                if (!stopUpdatingMessage || msg != "Server is running ✅") {
                    uiMessage = msg
                }
                isServerRunning = running
                if (running && !stopUpdatingMessage) {
                    stopUpdatingMessage = true
                }
            }
            delay(2000)
        }
    }



    LaunchedEffect(isServerRunning) {
        if (isServerRunning) {
            focusRequesterPhone.requestFocus()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!binaryInstalled) {
            Text("❌ Binary not installed", color = isRed, modifier = Modifier.padding(8.dp))
            Text("Login is unavailable", color = isRed)
        }

        uiMessage?.let {
            Text(it, color = Color.Gray, modifier = Modifier.padding(8.dp))
        }

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { newValue ->
                            val filtered = newValue.text.filter { it.isDigit() }
                            phoneNumber = newValue.copy(text = filtered)
                        },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            if (phoneNumber.text.isNotBlank() && !isLoadingOtpSend) {
                                isLoadingOtpSend = true
                                sendOtp(
                                    context = context,
                                    phoneNumber = phoneNumber.text,
                                    baseURL = baseURL,
                                    onMessageUpdate = { uiMessage = it }
                                ) { isLoadingOtpSend = false }
                            }
                            focusRequesterOtp.requestFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterPhone)
                            .onKeyEvent { handleKeyEvent(it, focusManager) }
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isLoadingOtpSend = true
                            sendOtp(
                                context = context,
                                phoneNumber = phoneNumber.text,
                                baseURL = baseURL,
                                onMessageUpdate = { uiMessage = it }
                            ) { isLoadingOtpSend = false }
                        },
                        enabled = !isLoadingOtpSend,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoadingOtpSend)
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        else Text("Send OTP")
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { newValue ->
                            val filtered = newValue.text.filter { it.isDigit() }
                            otpCode = newValue.copy(text = filtered)
                        },
                        label = { Text("OTP Code") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (otpCode.text.isNotBlank() && !isLoadingOtpVerify) {
                                isLoadingOtpVerify = true
                                verifyOtpAndProceed(
                                    context,
                                    phoneNumber.text,
                                    otpCode.text,
                                    baseURL,
                                    onMessageUpdate = { uiMessage = it },
                                    onFinished = { success ->
                                        isLoadingOtpVerify = false
                                        if (success) onCompleteStep()
                                    }
                                )
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterOtp)
                            .onKeyEvent { handleKeyEvent(it, focusManager) }
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isLoadingOtpVerify = true
                            verifyOtpAndProceed(
                                context,
                                phoneNumber.text,
                                otpCode.text,
                                baseURL,
                                onMessageUpdate = { uiMessage = it },
                                onFinished = { success ->
                                    isLoadingOtpVerify = false
                                    if (success) onCompleteStep()
                                }
                            )
                        },
                        enabled = !isLoadingOtpVerify,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoadingOtpVerify)
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        else Text("Verify OTP")
                    }
                }
            }
        } else {

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    val filtered = newValue.text.filter { it.isDigit() }
                    phoneNumber = newValue.copy(text = filtered)
                },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    if (phoneNumber.text.isNotBlank() && !isLoadingOtpSend) {
                        isLoadingOtpSend = true
                        sendOtp(
                            context = context,
                            phoneNumber = phoneNumber.text,
                            baseURL = baseURL,
                            onMessageUpdate = { uiMessage = it }
                        ) { isLoadingOtpSend = false }
                    }
                    focusRequesterOtp.requestFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesterPhone)
                    .onKeyEvent { handleKeyEvent(it, focusManager) }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoadingOtpSend = true
                    sendOtp(
                        context = context,
                        phoneNumber = phoneNumber.text,
                        baseURL = baseURL,
                        onMessageUpdate = { uiMessage = it }
                    ) { isLoadingOtpSend = false }
                },
                enabled = !isLoadingOtpSend,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoadingOtpSend)
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Send OTP")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { newValue ->
                    val filtered = newValue.text.filter { it.isDigit() }
                    otpCode = newValue.copy(text = filtered)
                },
                label = { Text("OTP Code") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (otpCode.text.isNotBlank() && !isLoadingOtpVerify) {
                        isLoadingOtpVerify = true
                        verifyOtpAndProceed(
                            context,
                            phoneNumber.text,
                            otpCode.text,
                            baseURL,
                            onMessageUpdate = { uiMessage = it },
                            onFinished = { success ->
                                isLoadingOtpVerify = false
                                if (success) onCompleteStep()
                            }
                        )
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesterOtp)
                    .onKeyEvent { handleKeyEvent(it, focusManager) }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoadingOtpVerify = true
                    verifyOtpAndProceed(
                        context,
                        phoneNumber.text,
                        otpCode.text,
                        baseURL,
                        onMessageUpdate = { uiMessage = it },
                        onFinished = { success ->
                            isLoadingOtpVerify = false
                            if (success) onCompleteStep()
                        }
                    )
                },
                enabled = !isLoadingOtpVerify,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoadingOtpVerify)
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Verify OTP")
            }
        }
    }
}

private fun sendOtp(
    context: Context,
    phoneNumber: String,
    baseURL: String,
    onMessageUpdate: (String) -> Unit,
    onFinished: () -> Unit
) {
    if (phoneNumber.isBlank()) {
        onMessageUpdate("Enter phone number")
        onFinished()
        return
    }

    onMessageUpdate("Sending OTP to $phoneNumber")
    Executors.newSingleThreadExecutor().execute {
        try {
            val url = URL("$baseURL/login/sendOTP")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = "{\"number\": \"+91$phoneNumber\"}"
            conn.outputStream.use { it.write(body.toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            Log.d("OTP_SEND", response)

            (context as Activity).runOnUiThread {
                onMessageUpdate("OTP Sent ✅")
                Toast.makeText(context, "OTP Sent!", Toast.LENGTH_SHORT).show()
                onFinished()
            }
        } catch (e: Exception) {
            Log.e("OTP", "Send error", e)
            (context as Activity).runOnUiThread {
                onMessageUpdate("Failed to send OTP ❌")
                Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                onFinished()
            }
        }
    }
}

private fun verifyOtpAndProceed(
    context: Context,
    phoneNumber: String,
    otp: String,
    baseURL: String,
    onMessageUpdate: (String) -> Unit,
    onFinished: (Boolean) -> Unit
) {
    if (otp.isBlank()) {
        onMessageUpdate("Enter OTP")
        onFinished(false)
        return
    }

    onMessageUpdate("Verifying OTP…")
    Executors.newSingleThreadExecutor().execute {
        try {
            val url = URL("$baseURL/login/verifyOTP")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val json = "{\"number\": \"+91$phoneNumber\", \"otp\": \"$otp\"}"
            conn.outputStream.use { it.write(json.toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            Log.d("OTP_VERIFY", response)
            val status = JSONObject(response).optString("status", "fail")

            (context as Activity).runOnUiThread {
                if (status == "success") {
                    onMessageUpdate("OTP Verified ✅")
                    Toast.makeText(context, "Welcome!", Toast.LENGTH_SHORT).show()
                    onFinished(true)
                } else {
                    onMessageUpdate("Invalid OTP ❌")
                    Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    onFinished(false)
                }
            }
        } catch (e: Exception) {
            Log.e("OTP", "Verify error", e)
            (context as Activity).runOnUiThread {
                onMessageUpdate("Verification failed ❌")
                Toast.makeText(context, "Verification failed", Toast.LENGTH_SHORT).show()
                onFinished(false)
            }
        }
    }
}

private fun checkServerStatus(
    url: String,
    onMessageUpdate: (String, Boolean) -> Unit
) {
    Executors.newSingleThreadExecutor().execute {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val code = conn.responseCode
            when (code) {
                200 -> onMessageUpdate("Server is running ✅", true)
                405 -> onMessageUpdate("DNS issue, please fix network ⚠️", false)
                else -> onMessageUpdate("Server not running ❌", false)
            }
        } catch (_: Exception) {
//            onMessageUpdate("Server is not running. If issue persists, go to main screen to debug.", false)
        }
    }
}

private fun handleKeyEvent(event: KeyEvent, focusManager: FocusManager): Boolean {
    if (event.type == KeyEventType.KeyDown) {
        when (event.key) {
            Key.DirectionDown -> {
                focusManager.moveFocus(FocusDirection.Down)
                return true
            }

            Key.DirectionUp -> {
                focusManager.moveFocus(FocusDirection.Up)
                return true
            }
        }
    }
    return false
}