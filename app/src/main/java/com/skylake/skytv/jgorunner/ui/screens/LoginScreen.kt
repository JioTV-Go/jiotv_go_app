package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.data.SkySharedPref
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(context: Context) {
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    val glowColor = Color.Green
    val preferenceManager = SkySharedPref.getInstance(context)

    var isUsingOtp by remember { mutableStateOf(true) }

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isGlowing by rememberUpdatedState(newValue = run {
        preferenceManager.myPrefs.serveLocal
    })

    // Focus requesters for managing focus on fields
    val phoneNumberFocusRequester = remember { FocusRequester() }
    val otpCodeFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            phoneNumberFocusRequester.requestFocus()
            Log.d("PreferenceCheck", "isFlagSetForLOCAL: $isGlowing")
            delay(3000) // Delay for 3 seconds
        }
    }

    // Function to draw the watermark
    @Composable
    fun Watermark() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = -30f)
                .alpha(0.1f)
        ) {
            Text(
                text = "NOT WORKING - EXPERIMENTAL",
                fontSize = 50.sp,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    // Main UI layout
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login",
                fontSize = 24.sp,
                fontFamily = customFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                style = if (isGlowing) {
                    TextStyle(
                        shadow = Shadow(
                            color = glowColor,
                            blurRadius = 30f,
                            offset = Offset(0f, 0f)
                        )
                    )
                } else {
                    TextStyle.Default
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tab layout to switch between OTP and Password
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        isUsingOtp = true
                        // Request focus for phone number when switching to OTP
                        phoneNumberFocusRequester.requestFocus()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Use OTP")
                    }
                }
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        isUsingOtp = false
                        // Request focus for phone number when switching to Password
                        phoneNumberFocusRequester.requestFocus()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Use Password")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUsingOtp) {
                // OTP Login UI
                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            otpCodeFocusRequester.requestFocus() // Move focus to OTP code
                        }
                    ),
                    modifier = Modifier.focusRequester(phoneNumberFocusRequester) // Attach focus requester
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    sendOtp(context, phoneNumber)
                }) {
                    Text(text = "Send OTP")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("OTP Code") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            verifyOtp(context, phoneNumber, otpCode)
                        }
                    ),
                    modifier = Modifier.focusRequester(otpCodeFocusRequester) // Attach focus requester
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    verifyOtp(context, phoneNumber, otpCode)
                }) {
                    Text(text = "Verify OTP")
                }

            } else {
                // Password Login UI
                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            passwordFocusRequester.requestFocus() // Move focus to password field
                        }
                    ),
                    modifier = Modifier.focusRequester(phoneNumberFocusRequester) // Attach focus requester
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            login(context, phoneNumber, password)
                        }
                    ),
                    modifier = Modifier.focusRequester(passwordFocusRequester) // Attach focus requester
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    login(context, phoneNumber, password)
                }) {
                    Text(text = "Login")
                }
            }
        }

        Watermark()
    }
}

private fun sendOtp(context: Context, phoneNumber: String) {
    if (phoneNumber.isNotBlank()) {
        Toast.makeText(context, "Sending OTP to $phoneNumber", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Please enter your phone number", Toast.LENGTH_SHORT).show()
    }
}

private fun verifyOtp(context: Context, phoneNumber: String, otpCode: String) {
    if (otpCode.isNotBlank()) {
        Toast.makeText(context, "Verifying OTP: $otpCode for $phoneNumber", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Please enter the OTP", Toast.LENGTH_SHORT).show()
    }
}

private fun login(context: Context, phoneNumber: String, password: String) {
    if (phoneNumber.isNotBlank() && password.isNotBlank()) {
        Toast.makeText(context, "Logging in with $phoneNumber", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Please enter your phone number and password", Toast.LENGTH_SHORT).show()
    }
}
