package com.skylake.skytv.jgorunner.ui.screens

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.DirectionsRun
import androidx.compose.material.icons.automirrored.twotone.ExitToApp
import androidx.compose.material.icons.twotone.Cast
import androidx.compose.material.icons.twotone.CastConnected
import androidx.compose.material.icons.twotone.Deblur
import androidx.compose.material.icons.twotone.DirectionsRun
import androidx.compose.material.icons.twotone.Landscape
import androidx.compose.material.icons.twotone.LiveTv
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material.icons.twotone.PlayCircleFilled
import androidx.compose.material.icons.twotone.PlayCircleOutline
import androidx.compose.material.icons.twotone.ResetTv
import androidx.compose.material.icons.twotone.Start
import androidx.compose.material.icons.twotone.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R
import com.skylake.skytv.jgorunner.ui.components.ButtonContent
import com.skylake.skytv.jgorunner.ui.components.ButtonContentCust
import kotlinx.coroutines.launch

private val customFontFamily = FontFamily(
    Font(R.font.chakrapetch_bold)
)

@Composable
fun HomeScreen(
    title: String,
    titleShouldGlow: Boolean,
    isServerRunning: Boolean,
    publicJTVServerURL: String,
    outputText: String,
    onRunServerButtonClick: () -> Unit,
    onStopServerButtonClick: () -> Unit,
    onRunIPTVButtonClick: () -> Unit,
    onWebTVButtonClick: () -> Unit,
    onDebugButtonClick: () -> Unit,
    onExitButtonClick: () -> Unit
) {

    val context = LocalContext.current
    val regex = Regex("http://(?:\\d+\\.\\d+\\.\\d+\\.\\d+|localhost)(:\\d+)?")
    fun updateURL(url: String): String {
        return url.replace(regex) { matchResult ->
            val port = matchResult.groups[1]?.value ?: ""
            "http://localhost$port"
        }.replace("/playlist.m3u", "")
    }
    val baseUrl = updateURL(publicJTVServerURL)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(75.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl))
                    Log.d("DND",baseUrl)
                    val packageName = "com.android.chrome"
                    val pm = context.packageManager
                    try {
                        pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                        intent.setPackage(packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Toast.makeText(context, "Chrome not found. Opening in default browser.", Toast.LENGTH_SHORT).show()
                    }
                    context.startActivity(intent)
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontFamily = customFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            style = if (titleShouldGlow) {
                TextStyle(
                    shadow = Shadow(
                        color = Color.Green,
                        blurRadius = 30f,
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                )

            } else {
                TextStyle.Default
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                .background(
                    if (titleShouldGlow) Color(
                        0xFFA5D6A7
                    ) else MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(8.dp)
                )
                .padding(5.dp)
        ) {
            val coroutineScope = rememberCoroutineScope()
            val clipboardManager = LocalClipboard.current

            Text(
                text = publicJTVServerURL,
                fontSize = 16.sp,
                color = if (titleShouldGlow) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        coroutineScope.launch {
                            val clipData = ClipData.newPlainText("label", publicJTVServerURL)
                            clipboardManager.setClipEntry(ClipEntry(clipData))
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RunServerButton(enabled = isServerRunning.not(), titleShouldGlow = titleShouldGlow) {
                onRunServerButtonClick()
            }
            StopServerButton(isServerRunning = isServerRunning) {
                onStopServerButtonClick()
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RunIPTVButton {
                onRunIPTVButtonClick()
            }
            DebugButton(
                enabled = isServerRunning
            ) {
                onDebugButtonClick()
            }
            WebTVButton(
                enabled = isServerRunning
            ) {
                onWebTVButtonClick()
            }
            ExitButton {
                onExitButtonClick()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.alpha(if (isServerRunning) 1f else 0f)
        ) {
            val outputScrollState = rememberScrollState()

            // Scroll to the bottom whenever the text changes (recomposition)
            LaunchedEffect(outputText) {
                outputScrollState.animateScrollTo(outputScrollState.maxValue)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = outputText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.Black)
                        .padding(8.dp)
                        .verticalScroll(outputScrollState)
                )
            }


        }
    }
}

@Composable
fun RowScope.RunServerButton(
    enabled: Boolean = true,
    titleShouldGlow: Boolean = true,
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .weight(3f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp),
        enabled = enabled
    ) {
        val loginState = if (titleShouldGlow) "Server is Running" else "Server is Running | Login Error"
        val text = if (enabled) "Run Server" else loginState
        ButtonContentCust(
            text = text,
            icon = if (enabled) Icons.TwoTone.PlayCircleOutline else Icons.AutoMirrored.TwoTone.DirectionsRun,
            noicon = false,
            isEnabled = enabled
        )
    }
}

@Composable
fun RowScope.StopServerButton(
    isServerRunning: Boolean,
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val colorRED = Color(0xFFFF4444)
    val isFocused = remember { mutableStateOf(false) }

    buttonColor.value = when {
        isServerRunning -> colorRED
        isFocused.value -> colorSECOND
        else -> colorPRIME
    }

    Button(
        onClick = {
            onClick()
        },
        enabled = isServerRunning,
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Stop", Icons.TwoTone.Stop) // Different icon
    }
}

@Composable
fun RowScope.RunIPTVButton(
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Run IPTV", Icons.TwoTone.ResetTv)
    }
}

@Composable
fun RowScope.WebTVButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }
    Button(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp),
        enabled = enabled
    ) {
        ButtonContent("Web TV", Icons.TwoTone.LiveTv)
    }
}

@Composable
fun RowScope.DebugButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }
    Button(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp),
        enabled = enabled
    ) {
        ButtonContent("TV", Icons.TwoTone.Landscape)
    }
}

@Composable
fun RowScope.ExitButton(
    onClick: () -> Unit
) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val colorBORDER = Color(0xFFFFD700)
    val isFocused = remember { mutableStateOf(false) }
    Button(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                buttonColor.value = if (focusState.isFocused) colorSECOND else colorPRIME
            },
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused.value) BorderStroke(2.dp, colorBORDER) else null,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Exit", Icons.AutoMirrored.TwoTone.ExitToApp)
    }
}