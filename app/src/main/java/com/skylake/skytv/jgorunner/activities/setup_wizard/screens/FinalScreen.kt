package com.skylake.skytv.jgorunner.activities.setup_wizard.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.R

private val customFontFamily = FontFamily(
    Font(R.font.chakrapetch_bold)
)

@Composable
fun FinalScreen(isDark: Boolean) {
    if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)
    val textSub = if (isDark) Color(0xFF9EA1F9) else Color(0xFF5E5AF5)
    if (isDark) Color(0xFF7E80C9) else Color(0xFF7C83EB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Streaming made simple.",
            fontSize = 18.sp,
            color = textSub,
            fontWeight = FontWeight.Medium
        )
    }
}
