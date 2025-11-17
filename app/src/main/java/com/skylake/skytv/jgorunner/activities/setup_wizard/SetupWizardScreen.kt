package com.skylake.skytv.jgorunner.activities.setup_wizard

import android.content.Intent
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skylake.skytv.jgorunner.activities.MainActivity
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.BinarySetup
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.FinalScreen
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.LoginSetup
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.OperationModeSetup
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.PermissionSetup
import com.skylake.skytv.jgorunner.activities.setup_wizard.screens.WelcomeScreen
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.utils.CustButton
import com.skylake.skytv.jgorunner.utils.CustOutlinedButton
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun InitialSetupWizard(
    preferenceManager: SkySharedPref,
    onComplete: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val adaptiveGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0A0F),
                Color(0xFF141427),
                Color(0xFF1E1E2F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF6F6F8),
                Color(0xFFEAEAF2),
                Color(0xFFD8D8E0)
            )
        )
    }

    val accentColor = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
    val cardColor = if (isDark) Color(0xFF1E1E2B) else Color(0xFFF9F9FF)
    val textPrimary = if (isDark) Color(0xFFECECFB) else Color(0xFF1C1C2E)
    if (isDark) Color(0xFFB3B6F2) else Color(0xFF4F46E5)

    fun parade(): List<Party> {
        val party = Party(
            speed = 10f,
            maxSpeed = 45f,
            damping = 0.9f,
            angle = Angle.RIGHT - 45,
            spread = Spread.SMALL,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 2.seconds).perSecond(25),
            position = Position.Relative(0.0, 0.5)
        )

        return listOf(
            party,
            party.copy(
                angle = party.angle - 90,
                position = Position.Relative(1.0, 0.5)
            )
        )
    }


    BackHandler(enabled = currentStep > 0) {
        currentStep--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(adaptiveGradient)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn() + slideInVertically { it / 2 } togetherWith
                                fadeOut() + slideOutVertically { -it / 2 }
                    }
                ) { step ->
                    Text(
                        text = when (step) {
                            0 -> "Welcome"
                            1 -> "Operation Mode"
                            2 -> "App Permissions"
                            3 -> "Download Binary"
                            4 -> "Login"
                            5 -> "Setup Complete 🎉"
                            else -> ""
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 24.dp),
                        lineHeight = 36.sp
                    )
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(200))
                    },
                    label = "stepTransition"
                ) { step ->
                    when (step) {
                        0 -> WelcomeScreen(isDark)
                        1 -> OperationModeSetup(preferenceManager, isDark)
                        2 -> PermissionSetup(isDark)
                        3 -> BinarySetup(preferenceManager, isDark) { currentStep++ }
                        4 -> LoginSetup(preferenceManager, isDark) { currentStep++ }
                        5 -> FinalScreen(isDark)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        CustOutlinedButton(
                            onClick = { currentStep-- },
                            border = BorderStroke(1.5.dp, accentColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accentColor
                            )
                        ) { Text("Back") }
                    } else {
                        Spacer(modifier = Modifier.width(100.dp))
                    }

                    when (currentStep) {
                        0 -> {
                            CustButton(
                                onClick = { currentStep++ },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Get Started", color = Color.White)
                            }
                        }

                        in 1..4 -> {
                            CustButton(
                                onClick = { currentStep++ },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Next", color = Color.White)
                            }
                        }

                        else -> {
                            CustButton(
                                onClick = {
                                    preferenceManager.myPrefs.setupPending = false
                                    preferenceManager.savePreferencesQuick()

                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)

                                    Process.killProcess(Process.myPid())
                                    onComplete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Watch TV", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (currentStep == 5) {
            ConfettiKit(
                modifier = Modifier
                    .matchParentSize(),
                parties = parade()
            )
        }
    }
}