package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R

private val customFontFamily = FontFamily(
    Font(R.font.chakrapetch_bold)
)

@Composable
fun WelcomeScreen(
    isDark: Boolean = isSystemInDarkTheme()
) {
    if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
    if (isDark) Color(0xFFECECFB) else Color(0xFF1C1C2E)
    val subText = if (isDark) Color(0xFFB5B8E8) else Color(0xFF555579)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(75.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "JTV-GO SERVER",
            fontSize = 24.sp,
            fontFamily = customFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            style =
                TextStyle(
                    shadow = Shadow(
                        color = Color.Green,
                        blurRadius = 30f,
                        offset = Offset(0f, 0f)
                    )
                )
        )

        Text(
            text = "Get ready to seamlessly stream Live TV channels",
            color = subText,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

    }
}


