package com.skylake.skytv.jgorunner.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.activities.MainActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.utils.CustButton
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.random.Random

@Composable
fun LoginScreen(context: Context) {
    val preferenceManager = SkySharedPref.getInstance(context)
    val scrollState = rememberScrollState()

    val localPORT by remember {
        mutableIntStateOf(preferenceManager.myPrefs.jtvGoServerPort)
    }
    val basefinURL = "http://localhost:$localPORT"

    val glowColors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta
    )
    val glowColor = remember { Animatable(glowColors[Random.nextInt(glowColors.size)]) }
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    var isUsingOtp by remember { mutableStateOf(true) }

    var phoneNumber by remember { mutableStateOf(TextFieldValue()) }
    var otpCode by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }

    val focusRequesterList = remember { List(3) { FocusRequester() } }
    val (phoneNumberFocusRequester, otpCodeFocusRequester, passwordFocusRequester) = focusRequesterList

    val focusManager = LocalFocusManager.current

    var isUrlAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(basefinURL) {
        isUrlAvailable(basefinURL) { isAvailable ->
            isUrlAvailable = isAvailable
        }
    }

    LaunchedEffect(Unit) {
        phoneNumberFocusRequester.requestFocus()
    }

    Box(modifier =
        Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login",
                fontSize = 24.sp,
                fontFamily = customFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(
                    shadow = Shadow(
                        color = glowColor.value,
                        blurRadius = 30f,
                        offset = Offset(0f, 0f)
                    )
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )


            if (!isUrlAvailable) {
                Text(
                    text = "Server is not started, Login function will not work",
                    color = Color.Red
                )
            }

            // Tab layout to switch between OTP and Password
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        isUsingOtp = true
                        phoneNumberFocusRequester.requestFocus()
                    }
                ) {
                    Text("Use OTP", modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        isUsingOtp = false
                        phoneNumberFocusRequester.requestFocus()
                    }
                ) {
                    Text("Use Password", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUsingOtp) {
                // OTP Login UI
                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { otpCodeFocusRequester.requestFocus() }),
                    modifier = Modifier
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

                Spacer(modifier = Modifier.height(8.dp))

                CustButton(onClick = {
                    sendOtp(
                        context,
                        phoneNumber.text,
                        basefinURL
                    )
                }) {
                    Text("Send OTP")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("OTP Code") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        verifyOtp(
                            context,
                            phoneNumber.text,
                            otpCode.text,
                            basefinURL
                        )
                    }),
                    modifier = Modifier
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

                Spacer(modifier = Modifier.height(8.dp))

                CustButton(onClick = {
                    verifyOtp(
                        context,
                        phoneNumber.text,
                        otpCode.text,
                        basefinURL
                    )
                }) {
                    Text("Verify OTP")
                }

            } else {
                // Password Login UI
                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    modifier = Modifier
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

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        login(
                            context,
                            phoneNumber.text,
                            password.text,
                            basefinURL
                        )
                    }),
                    modifier = Modifier
                        .focusRequester(passwordFocusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            handleKeyEvent(
                                event = event,
                                focusManager = focusManager,
                                focusRequesterList = focusRequesterList,
                                currentFieldValue = password.text,
                                cursorPosition = password.selection.end
                            )
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustButton(onClick = {
                    login(
                        context,
                        phoneNumber.text,
                        password.text,
                        basefinURL
                    )
                }) {
                    Text("Login")
                }
            }
        }
    }
}

private fun handleKeyEvent(
    event: KeyEvent,
    focusManager: FocusManager,
    focusRequesterList: List<FocusRequester>,
    currentFieldValue: String,
    cursorPosition: Int
): Boolean {
    if (event.type == KeyEventType.KeyDown) {
        when (event.key) {
            Key.DirectionDown -> {
                if (cursorPosition == currentFieldValue.length) {
                    // Cursor is at the end of the field, move focus to the next field
                    focusManager.moveFocus(FocusDirection.Down)
                    return true
                }
            }

            Key.DirectionUp -> {
                if (cursorPosition == 0) {
                    // Cursor is at the start of the field, move focus to the previous field
                    focusManager.moveFocus(FocusDirection.Up)
                    return true
                }
            }
        }
    }
    return false
}

private fun sendOtp(context: Context, phoneNumber: String, baseURL: String) {
    if (phoneNumber.isNotBlank()) {
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
                    handleSendOtpResponse(context, response.toString())
                }

            } catch (e: Exception) {
                Log.e("OTP", "Error sending OTP", e)
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Toast.makeText(context, "Sending OTP to $phoneNumber", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
    }
}

// Handle the OTP sending response
private fun handleSendOtpResponse(context: Context, result: String?) {
    if (result != null) {
        if (result.contains("true", ignoreCase = true)) {
            Toast.makeText(context, "OTP Sent: Successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "OTP Sent: $result", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
    }
}


private fun verifyOtp(context: Context, phoneNumber: String, otpCode: String, baseURL: String) {
    if (otpCode.isNotBlank()) {
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
                    handleVerifyOtpResponse(context, response.toString())
                }

            } catch (e: Exception) {
                Log.e("OTP", "Error verifying OTP", e)
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Toast.makeText(context, "Verifying OTP: $otpCode for $phoneNumber", Toast.LENGTH_SHORT)
            .show()
    } else {
        Toast.makeText(context, "Please enter the OTP", Toast.LENGTH_SHORT).show()
    }
}

private fun handleVerifyOtpResponse(context: Context, result: String?) {
    if (result != null) {
        try {
            val jsonObject = JSONObject(result)
            val status = jsonObject.optString("status")
            if (status == "success") {
                Toast.makeText(context, "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Failed to verify OTP", Toast.LENGTH_SHORT).show()
    }
}


private fun login(context: Context, phoneNumber: String, password: String, baseURL: String) {
    if (phoneNumber.isNotBlank() && password.isNotBlank()) {
        Thread {
            try {
                val url = URL("$baseURL/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonInputString =
                    "{\"username\": \"$phoneNumber\", \"password\": \"$password\"}"

                conn.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    (context as Activity).runOnUiThread {
                        handleVerifyPasswordResponse(context, "success")
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        handleVerifyPasswordResponse(context, "failure")
                    }
                }

                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                (context as Activity).runOnUiThread {
                    handleVerifyPasswordResponse(context, "failure")
                }
            }
        }.start()

        Toast.makeText(context, "Logging in with $phoneNumber", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Please enter your phone number and password", Toast.LENGTH_SHORT)
            .show()
    }
}

private fun handleVerifyPasswordResponse(context: Context, result: String) {
    if (result == "success") {
        Toast.makeText(context, "Login success. Enjoy!", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Login failed! Please check your credentials.", Toast.LENGTH_SHORT)
            .show()
    }
}

private fun isUrlAvailable(url: String, onResult: (Boolean) -> Unit) {
    val executorService = Executors.newSingleThreadExecutor()
    executorService.execute {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod =
                "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            onResult(responseCode == HttpURLConnection.HTTP_OK)
        } catch (e: Exception) {
            onResult(false)
        }
    }
}