package com.skylake.skytv.jgorunner.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun FocusableContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    focusColor: Color = Color(0xFFFFD700),
    borderWidth: Dp = 3.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable (Modifier, MutableInteractionSource, Boolean) -> Unit
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusModifier = Modifier
        .drawBehind {
            if (isFocused) {
                val strokeWidthPx = borderWidth.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color = focusColor,
                    style = Stroke(strokeWidthPx)
                )
            }
        }
        .focusable(enabled, interactionSource)

    Box(modifier = modifier.then(focusModifier)) {
        content(Modifier, interactionSource, isFocused)
    }
}

@Composable
fun CustButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    focusColor: Color = Color(0xFFFFD700), // gold
    borderWidth: Dp = 3.dp, // bold border
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) focusColor else Color.Transparent

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        modifier = modifier
            .border(BorderStroke(borderWidth, borderColor), shape)
            .focusable(enabled, interactionSource)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        content()
    }
}

@Composable
fun CustOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    focusColor: Color = Color(0xFFFFD700),
    borderWidth: Dp = 3.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val outerBorderColor = if (isFocused) focusColor else Color.Transparent

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        modifier = modifier
            .border(BorderStroke(borderWidth, outerBorderColor), shape)
            .focusable(enabled, interactionSource)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        content()
    }
}
