package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.data.SkySharedPref


@Composable
fun OperationModeSetup(preferenceManager: SkySharedPref, isDark: Boolean) {
    val inactiveText = if (isDark) Color(0xFFAAAAEE) else Color(0xFF5C5CA8)
    val activeText = if (isDark) Color.White else Color(0xFF3F3DD9)
    var selectedIndex by remember { mutableIntStateOf(preferenceManager.myPrefs.operationMODE) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.Center) {
            SingleChoiceSegmentedButtonRow {

                GlowingSimpleButton(
                    index = 0,
                    selectedIndex = selectedIndex,
                    onSelect = {
                        selectedIndex = 0
                        preferenceManager.myPrefs.apply {
                            operationMODE = 0
                            autoStartServer = true
                            loginChk = true
                            jtvGoServerPort = 5350
                            iptvAppPackageName = "tvzone"
                        }
                        preferenceManager.savePreferences()
                    },
                    activeText = activeText,
                    inactiveText = inactiveText
                )

                SegmentedButton(
                    selected = selectedIndex == 1,
                    onClick = {
                        selectedIndex = 1
                        preferenceManager.myPrefs.apply {
                            operationMODE = 1
                            autoStartServer = true
                            loginChk = true
                            iptvAppPackageName = ""
                            startTvAutomatically = false
                        }
                        preferenceManager.savePreferences()
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.focusable(),
                    label = {
                        Text(
                            "Expert",
                            color = if (selectedIndex == 1) activeText else inactiveText
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        ModeDescription(selectedIndex, isDark)
    }
}

@Composable
fun SingleChoiceSegmentedButtonRowScope.GlowingSimpleButton(
    index: Int,
    selectedIndex: Int,
    onSelect: () -> Unit,
    activeText: Color,
    inactiveText: Color
) {
    val selected = selectedIndex == index
    val expertSelected = selectedIndex == 1
    val goldLight = Color(0xFFFFE066)
    val goldMedium = Color(0xFFFFD700)
    val goldDeep = Color(0xFFFFB300)

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = -250f,
        targetValue = 250f,
        animationSpec = if (!expertSelected)
            infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        else
            infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = ""
    )

    val safeGradient = if (Build.VERSION.SDK_INT >= 26 && !expertSelected) {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                goldDeep.copy(alpha = 0.3f),
                goldMedium.copy(alpha = 0.7f),
                goldLight.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(offsetX, 0f),
            end = Offset(offsetX + 200f, 0f)
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val backgroundModifier =
        if (!selected && !expertSelected) {
            Modifier
                .background(
                    brush = safeGradient,
                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                )
        } else Modifier

    Box(modifier = backgroundModifier) {
        this@GlowingSimpleButton.SegmentedButton(
            selected = selected,
            onClick = onSelect,
            shape = SegmentedButtonDefaults.itemShape(index, 2),
            label = {
                Text(
                    "Simple",
                    color = if (selected) activeText else inactiveText
                )
            }
        )
    }
}


@Composable
fun ModeDescription(index: Int, isDark: Boolean) {
    val accent = if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val subText = if (isDark) Color(0xFF8E90D9) else Color(0xFF6D6FF5)

    val descriptionList: @Composable () -> Unit = when (index) {
        0 -> {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "• Automatically applies best settings",
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "• Uses a clean & simple TV layout",
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        1 -> {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "• Provides complete control over settings",
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "• Choose preferred IPTV player",
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "• May require additional settings to be tuned",
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        else -> {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Recommended mode: ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Simple Mode")
                            }
                        },
                        color = subText,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = when (index) {
            0 -> "Simple Mode"
            1 -> "Expert Mode"
            else -> "Select a Mode"
        }

        Text(
            text = title,
            color = accent,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))
        descriptionList()
    }
}
