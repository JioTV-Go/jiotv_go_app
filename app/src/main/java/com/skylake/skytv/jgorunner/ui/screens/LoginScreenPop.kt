package com.skylake.skytv.jgorunner.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.MainActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.random.Random

@Composable
fun LoginScreenPop(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    context: Context
) {
    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 12.dp,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            LoginDialogContent(context, onDismissRequest)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginDialogContent(
    context: Context,
    onDismissRequest: () -> Unit
) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val scrollState = rememberScrollState()
    val localPORT by remember { mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort) }
    val basefinURL = "http://localhost:$localPORT"

    val glowColors = remember {
        listOf(
            Color.Red,
            Color.Green,
            Color.Blue,
            Color.Yellow,
            Color.Cyan,
            Color.Magenta
        )
    }
    val glowColor = remember { Animatable(glowColors[Random.nextInt(glowColors.size)]) }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    var isUsingOtp by remember { mutableStateOf(true) }

    var phoneNumber by remember { mutableStateOf(TextFieldValue()) }
    var otpCode by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }


    var isLoadingOtpSend by remember { mutableStateOf(false) }
    var isLoadingOtpVerify by remember { mutableStateOf(false) }
    var isLoadingLogin by remember { mutableStateOf(false) }


    var uiStatusMessage by remember { mutableStateOf<String?>(null) }


    val updateUiStatusMessage: (String?) -> Unit = { message ->
        uiStatusMessage = message
    }


    val focusRequesterList = remember { List(3) { FocusRequester() } }
    val (phoneNumberFocusRequester, otpCodeFocusRequester, passwordFocusRequester) = focusRequesterList

    val focusManager = LocalFocusManager.current

    var isUrlAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(basefinURL) {
        isUrlAvailable(basefinURL) { isAvailable -> isUrlAvailable = isAvailable }
    }

    LaunchedEffect(Unit) { phoneNumberFocusRequester.requestFocus() }


    val colorBORDER = Color(0xFFFFD700)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Login",
            fontSize = 32.sp,
            fontFamily = customFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(
                shadow = Shadow(
                    color = glowColor.value,
                    blurRadius = 30f,
                    offset = Offset(0f, 0f)
                )
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )


        if (uiStatusMessage != null) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = uiStatusMessage!!,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!isUrlAvailable) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Server is not started, Login function will not work",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

//        var selectedTabIndex by remember { mutableStateOf(0) }
//        TabRow(
//            selectedTabIndex = selectedTabIndex,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Tab(
//                selected = selectedTabIndex == 0,
//                onClick = {
//                    selectedTabIndex = 0
//                    isUsingOtp = true
//                    phoneNumberFocusRequester.requestFocus()
//                    updateUiStatusMessage(null)
//                },
//                text = { Text("OTP", modifier = Modifier.padding(10.dp)) }
//            )
//            Tab(
//                selected = selectedTabIndex == 1,
//                onClick = {
//                    selectedTabIndex = 1
//                    isUsingOtp = false
//                    phoneNumberFocusRequester.requestFocus()
//                    updateUiStatusMessage(null)
//                },
//                text = { Text("Password", modifier = Modifier.padding(10.dp)) }
//            )
//        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                val filteredText = newValue.text.filter { it.isDigit() }
                phoneNumber = newValue.copy(text = filteredText)
            },
            label = { Text("Phone Number") },
            placeholder = { Text("Enter your 10-digit number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Icon") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onNext = { otpCodeFocusRequester.requestFocus() },onDone = {
                isLoadingOtpSend = true
                sendOtp(context, phoneNumber.text, basefinURL, updateUiStatusMessage) {
                    isLoadingOtpSend = false
                }
                otpCodeFocusRequester.requestFocus()}),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneNumberFocusRequester)
                .focusable()
                .onKeyEvent { event ->
                    handleKeyEvent(
                        event = event,
                        focusManager = focusManager,
                        focusRequesterList = focusRequesterList,
                        currentFieldValue = phoneNumber.text,
                        cursorPosition = phoneNumber.selection.end
                    )
                }
        )
        Spacer(modifier = Modifier.height(16.dp))


        var isSendOtpButtonFocused by remember { mutableStateOf(false) }
        Button(
            onClick = {
                isLoadingOtpSend = true
                sendOtp(context, phoneNumber.text, basefinURL, updateUiStatusMessage) {
                    isLoadingOtpSend = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isSendOtpButtonFocused = focusState.isFocused
                },
            enabled = !isLoadingOtpSend,
            shape = RoundedCornerShape(8.dp),
            border = if (isSendOtpButtonFocused) BorderStroke(2.dp, colorBORDER) else null,
        ) {
            if (isLoadingOtpSend) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send OTP")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { newValue ->
                val filteredText = newValue.text.filter { it.isDigit() }
                otpCode = newValue.copy(text = filteredText)
            },
            label = { Text("OTP Code") },
            placeholder = { Text("Enter the 6-digit OTP") },
            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = "OTP Icon") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                isLoadingOtpVerify = true
                verifyOtp(context, phoneNumber.text, otpCode.text, basefinURL, updateUiStatusMessage) {
                    isLoadingOtpVerify = false
                }
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(otpCodeFocusRequester)
                .focusable()
                .onKeyEvent { event ->
                    handleKeyEvent(
                        event = event,
                        focusManager = focusManager,
                        focusRequesterList = focusRequesterList,
                        currentFieldValue = otpCode.text,
                        cursorPosition = otpCode.selection.end
                    )
                }
        )

        Spacer(modifier = Modifier.height(16.dp))


        var isVerifyOtpButtonFocused by remember { mutableStateOf(false) }
        Button(
            onClick = {
                isLoadingOtpVerify = true
                verifyOtp(context, phoneNumber.text, otpCode.text, basefinURL, updateUiStatusMessage) {
                    isLoadingOtpVerify = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isVerifyOtpButtonFocused = focusState.isFocused
                },
            enabled = !isLoadingOtpVerify,
            shape = RoundedCornerShape(8.dp),
            border = if (isVerifyOtpButtonFocused) BorderStroke(2.dp, colorBORDER) else null,
        ) {
            if (isLoadingOtpVerify) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify OTP")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        var isCloseButtonFocused by remember { mutableStateOf(false) }
        TextButton(
            onClick = onDismissRequest,
            modifier = Modifier
                .align(Alignment.End)
                .onFocusChanged { focusState ->
                    isCloseButtonFocused = focusState.isFocused
                },
            shape = RoundedCornerShape(8.dp),
            border = if (isCloseButtonFocused) BorderStroke(2.dp, colorBORDER) else null,
        ) {
            Text("Close")
        }
    }
}


private fun handleKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    focusManager: FocusManager,
    focusRequesterList: List<FocusRequester>,
    currentFieldValue: String,
    cursorPosition: Int
): Boolean {
    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
        when (event.key) {
            androidx.compose.ui.input.key.Key.DirectionDown -> {
                if (cursorPosition == currentFieldValue.length) {
                    focusManager.moveFocus(FocusDirection.Down)
                    return true
                }
            }
            androidx.compose.ui.input.key.Key.DirectionUp -> {
                if (cursorPosition == 0) {
                    focusManager.moveFocus(FocusDirection.Up)
                    return true
                }
            }
        }
    }
    return false
}


private fun sendOtp(context: Context, phoneNumber: String, baseURL: String, onMessageUpdate: (String) -> Unit, onFinished: () -> Unit) {
    if (phoneNumber.isNotBlank()) {
        onMessageUpdate("Sending OTP to $phoneNumber")
        Toast.makeText(context, "Sending OTP to $phoneNumber", Toast.LENGTH_SHORT).show()
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                val url = URL("$baseURL/login/sendOTP")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonInputString = "{\"number\": \"+91$phoneNumber\"}"
                connection.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream, "utf-8")).use { br ->
                    var responseLine: String?
                    while (br.readLine().also { responseLine = it } != null) {
                        response.append(responseLine!!.trim())
                    }
                }

                (context as Activity).runOnUiThread {
                    handleSendOtpResponse(context, response.toString(), onMessageUpdate)
                    onFinished()
                }
            } catch (e: Exception) {
                Log.e("OTP", "Error sending OTP", e)
                (context as Activity).runOnUiThread {
                    onMessageUpdate("Failed to send OTP")
                    Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                    onFinished()
                }
            }
        }
    } else {
        onMessageUpdate("Please enter your phone number")
        Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
        onFinished()
    }
}

private fun handleSendOtpResponse(context: Context, result: String?, onMessageUpdate: (String) -> Unit) {
    if (result != null) {
        if (result.contains("true", ignoreCase = true)) {
            onMessageUpdate("OTP Sent: Successfully")
            Toast.makeText(context, "OTP Sent: Successfully", Toast.LENGTH_SHORT).show()
        } else {
            onMessageUpdate("OTP Sent: $result")
            Toast.makeText(context, "OTP Sent: $result", Toast.LENGTH_SHORT).show()
        }
    } else {
        onMessageUpdate("Failed to send OTP")
        Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
    }
}


private fun verifyOtp(context: Context, phoneNumber: String, otpCode: String, baseURL: String, onMessageUpdate: (String) -> Unit, onFinished: () -> Unit) {
    if (otpCode.isNotBlank()) {
        onMessageUpdate("Verifying OTP: $otpCode for $phoneNumber")
        Toast.makeText(context, "Verifying OTP: $otpCode for $phoneNumber", Toast.LENGTH_SHORT).show()
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                val url = URL("$baseURL/login/verifyOTP")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonInputString = "{\"number\": \"+91$phoneNumber\", \"otp\": \"$otpCode\"}"
                connection.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream, "utf-8")).use { br ->
                    var responseLine: String?
                    while (br.readLine().also { responseLine = it } != null) {
                        response.append(responseLine!!.trim())
                    }
                }

                (context as Activity).runOnUiThread {
                    handleVerifyOtpResponse(context, response.toString(), onMessageUpdate)
                    onFinished()
                }
            } catch (e: Exception) {
                Log.e("OTP", "Error verifying OTP", e)
                (context as Activity).runOnUiThread {
                    onMessageUpdate("Failed to verify OTP")
                    Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
                    onFinished()
                }
            }
        }
    } else {
        onMessageUpdate("Please enter the OTP")
        Toast.makeText(context, "Please enter the OTP", Toast.LENGTH_SHORT).show()
        onFinished()
    }
}

private fun handleVerifyOtpResponse(context: Context, result: String?, onMessageUpdate: (String) -> Unit) {
    if (result != null) {
        try {
            val jsonObject = JSONObject(result)
            val status = jsonObject.optString("status")
            if (status == "success") {
                onMessageUpdate("OTP Verified Successfully")
                Toast.makeText(context, "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            } else {
                onMessageUpdate("Failed to verify OTP")
                Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            onMessageUpdate("Error parsing response")
            Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
        }
    } else {
        onMessageUpdate("Failed to verify OTP")
        Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
    }
}

private fun login(context: Context, phoneNumber: String, password: String, baseURL: String, onMessageUpdate: (String) -> Unit, onFinished: () -> Unit) {
    if (phoneNumber.isNotBlank() && password.isNotBlank()) {
        onMessageUpdate("Logging in with $phoneNumber")
        Toast.makeText(context, "Logging in with $phoneNumber", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val url = URL("$baseURL/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonInputString = "{\"username\": \"$phoneNumber\", \"password\": \"$password\"}"

                conn.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    (context as Activity).runOnUiThread {
                        handleVerifyPasswordResponse(context, "success", onMessageUpdate)
                        onFinished()
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        handleVerifyPasswordResponse(context, "failure", onMessageUpdate)
                        onFinished()
                    }
                }

                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                (context as Activity).runOnUiThread {
                    handleVerifyPasswordResponse(context, "failure", onMessageUpdate)
                    onFinished()
                }
            }
        }.start()
    } else {
        onMessageUpdate("Please enter your phone number and password")
        Toast.makeText(context, "Please enter your phone number and password", Toast.LENGTH_SHORT).show()
        onFinished()
    }
}

private fun handleVerifyPasswordResponse(context: Context, result: String, onMessageUpdate: (String) -> Unit) {
    if (result == "success") {
        onMessageUpdate("Login success. Enjoy!")
        Toast.makeText(context, "Login success. Enjoy!", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
    } else {
        onMessageUpdate("Login failed! Please check your credentials.")
        Toast.makeText(context, "Login failed! Please check your credentials.", Toast.LENGTH_SHORT).show()
    }
}

private fun isUrlAvailable(url: String, onResult: (Boolean) -> Unit) {
    val executorService = Executors.newSingleThreadExecutor()
    executorService.execute {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            onResult(responseCode == HttpURLConnection.HTTP_OK)
        } catch (_: Exception) {
            onResult(false)
        }
    }
}
