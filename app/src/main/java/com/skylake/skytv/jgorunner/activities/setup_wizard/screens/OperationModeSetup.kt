package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.data.SkySharedPref

@Composable
fun OperationModeSetup(preferenceManager: SkySharedPref, isDark: Boolean) {
    var selectedIndex by remember { mutableIntStateOf(preferenceManager.myPrefs.operationMODE) }

    
    val configuration = LocalConfiguration.current
    val isLandscapeOrTv = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    
    val simpleModeDescriptions = if (isLandscapeOrTv) {
        listOf(
            "Automatically applies best settings",
            "Uses a clean & simple TV layout"
        )
    } else {
        listOf(
            "All auto set",
            "Clean TV layout",
            "Simple to use"
        )
    }

    val expertModeDescriptions = if (isLandscapeOrTv) {
        listOf(
            "Provides complete control over settings",
            "Choose preferred IPTV player",
            "May require additional settings to be tuned"
        )
    } else {
        listOf(
            "Full manual control",
            "Set IPTV player",
            "Manual tuning"
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            WizardOptionCard(
                title = "Simple Mode",
                descriptions = simpleModeDescriptions,
                isSelected = selectedIndex == 0,
                isRecommended = selectedIndex != 0 && selectedIndex != 1,
                isDark = isDark,
                onClick = {
                    selectedIndex = 0
                    preferenceManager.myPrefs.apply {
                        operationMODE = 0
                        operationUI = 0
                        autoStartServer = true
                        loginChk = true
                        jtvGoServerPort = 5350
                        iptvAppPackageName = "omni"
                    }
                    preferenceManager.savePreferences()
                }
            )

            
            WizardOptionCard(
                title = "Expert Mode",
                descriptions = expertModeDescriptions,
                isSelected = selectedIndex == 1,
                isRecommended = false,
                isDark = isDark,
                onClick = {
                    selectedIndex = 1
                    preferenceManager.myPrefs.apply {
                        operationMODE = 1
                        operationUI = 0
                        autoStartServer = true
                        loginChk = true
                        iptvAppPackageName = ""
                        startTvAutomatically = false
                    }
                    preferenceManager.savePreferences()
                }
            )
        }
    }
}

@Composable
fun WizardOptionCard(
    title: String,
    descriptions: List<String>,
    isSelected: Boolean,
    isRecommended: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val accent = if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val subText = if (isDark) Color(0xFF8E90D9) else Color(0xFF6D6FF5)
    val activeText = if (isDark) Color.White else Color(0xFF3F3DD9)

    var isFocused by remember { mutableStateOf(false) }

    
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart 
        ),
        label = "glow_offset"
    )

    val goldLight = Color(0xFFFFE066)
    val goldMedium = Color(0xFFFFD700)
    val goldDeep = Color(0xFFFFB300)

    val glowBrush = if (Build.VERSION.SDK_INT >= 26 && isRecommended) {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                goldDeep.copy(alpha = 0.6f),
                goldMedium.copy(alpha = 0.9f),
                goldLight.copy(alpha = 0.6f),
                Color.Transparent
            ),
            start = Offset(offsetX, 0f),
            end = Offset(offsetX + 300f, 0f)
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val outlineColor = if (isFocused) accent else Color.Transparent
    val cardBackground = if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent

    val borderModifier = when {
        isFocused -> Modifier.border(2.dp, outlineColor, MaterialTheme.shapes.medium)
        isRecommended && !isSelected -> Modifier.border(2.dp, glowBrush, MaterialTheme.shapes.medium)
        else -> Modifier.border(1.dp, subText.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(borderModifier)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
            contentColor = activeText
        )
    ) {
        
        Box(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    
                    .padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = accent,
                        unselectedColor = subText
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    descriptions.forEach { desc ->
                        Text(
                            text = "• $desc",
                            fontSize = 14.sp,
                            color = subText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            
            if (isRecommended && !isSelected) {
                Text(
                    text = "RECOMMENDED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = goldMedium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = goldDeep.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(bottomStart = 8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}