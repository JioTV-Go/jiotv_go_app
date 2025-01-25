package com.skylake.skytv.jgorunner.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.skylake.skytv.jgorunner.R
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.animation.Animatable
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.Support
import androidx.compose.runtime.*
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.ui.components.ButtonContent

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.skylake.skytv.jgorunner.activities.CastActivity
import com.skylake.skytv.jgorunner.activities.ExoplayerActivity

@Composable
fun DebugScreen(context: Context, onNavigate: (String) -> Unit) {
    val customFontFamily = FontFamily(Font(R.font.chakrapetch_bold))
    var isGlowing by remember { mutableStateOf(false) }
    val glowColors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta
    )
    val glowColor = remember { Animatable(glowColors.first()) }
    val preferenceManager = SkySharedPref.getInstance(context)
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            repeat(3) {
                val savedSwitchState = preferenceManager.myPrefs.serveLocal
                Log.d("PreferenceCheck", "isFlagSetForLOCAL: $savedSwitchState")
                isGlowing = savedSwitchState
                delay(10000)
            }
        }
    }

    LaunchedEffect(Unit) {
        var currentIndex = 0
        while (true) {
            val nextIndex = (currentIndex + 1) % glowColors.size
            glowColor.animateTo(
                targetValue = glowColors[nextIndex],
                animationSpec = tween(durationMillis = 1000)
            )
            currentIndex = nextIndex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JTV-GO SERVER",
            fontSize = 24.sp,
            fontFamily = customFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            style = if (isGlowing) {
                TextStyle(
                    shadow = Shadow(
                        color = glowColor.value,
                        blurRadius = 30f,
                        offset = Offset(0f, 0f)
                    )
                )
            } else {
                TextStyle.Default
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button5(context, onNavigate)
            Button6(context)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button7(context)
            Button8(context)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button1(context, onNavigate)
            Button2(context, onNavigate)
            Button3(context)
            Button4(context)

        }
    }
}


@Composable
fun RowScope.Button1(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    Button(
        onClick = {
            handleButton1Click(context,onNavigate)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Runner", Icons.AutoMirrored.Filled.DirectionsRun)
    }
}

@Composable
fun RowScope.Button2(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }
    val borderColor = remember { mutableStateOf(Color.Transparent) }

    Button(
        onClick = {
            handleButton2Click(context,onNavigate)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    borderColor.value = Color.White
                    colorSECOND
                } else {
                    borderColor.value = Color.Transparent
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Sys Info", Icons.Sharp.Info)
    }
}

@Composable
fun RowScope.Button3(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = MaterialTheme.colorScheme.secondary
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = { handleButton3Click(context) },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Support", Icons.Sharp.Support)
    }
}

@Composable
fun RowScope.Button4(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = { handleButton4Click(context) },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("GitHub", Icons.Default.Verified)
    }
}

@Composable
fun RowScope.Button5(context: Context, onNavigate: (String) -> Unit) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton5Click(context, onNavigate)
                  },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Login Exp.", Icons.Default.Verified)
    }
}

@Composable
fun RowScope.Button6(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton6Click(context)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("Exoplayer Debug", Icons.Default.PlayCircleOutline)
    }
}

@Composable
fun RowScope.Button7(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton7Click(context)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("---", Icons.Default.Terrain)
    }
}

@Composable
fun RowScope.Button8(context: Context) {
    val colorPRIME = MaterialTheme.colorScheme.primary
    val colorSECOND = colorPRIME.copy(alpha = 0.5f)
    val buttonColor = remember { mutableStateOf(colorPRIME) }

    Button(
        onClick = {
            handleButton8Click(context)
        },
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .onFocusChanged { focusState ->
                buttonColor.value = if (focusState.isFocused) {
                    colorSECOND
                } else {
                    colorPRIME
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor.value),
        contentPadding = PaddingValues(2.dp)
    ) {
        ButtonContent("CAST", Icons.Default.Cast)
    }
}




fun handleButton1Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Caution: Experimental!\n May be unstable.", Toast.LENGTH_SHORT).show()
    onNavigate("Runner")
}

fun handleButton2Click(context: Context, onNavigate: (String) -> Unit) {
    Toast.makeText(context, "Retrieving system information...", Toast.LENGTH_SHORT).show()
    onNavigate("Info")
}

fun handleButton3Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/3Uc1usW"))
    context.startActivity(intent)
}

fun handleButton4Click(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/JTV-GO-Server"))
    context.startActivity(intent)
}

fun handleButton5Click(context: Context, onNavigate: (String) -> Unit) {
    onNavigate("Login")
    Toast.makeText(context, "Pending Implementation", Toast.LENGTH_SHORT).show()
}

fun handleButton6Click(context: Context) {
    Toast.makeText(context, "Demo Stream Playing", Toast.LENGTH_SHORT).show()
    val intent = Intent(context, ExoplayerActivity::class.java)
    context.startActivity(intent)
}

fun handleButton7Click(context: Context) {
    Toast.makeText(context, "Pending Implementation", Toast.LENGTH_SHORT).show()
}

fun handleButton8Click(context: Context) {
    val intent = Intent(context, CastActivity::class.java)
    context.startActivity(intent)
}