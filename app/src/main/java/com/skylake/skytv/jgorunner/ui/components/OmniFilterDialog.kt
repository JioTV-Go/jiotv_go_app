package com.skylake.skytv.jgorunner.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun OmniFilterDialog(
    title: String,
    options: List<String>,
    selectedOptions: Set<String>,
    singleSelect: Boolean = true, 
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onReset: (() -> Unit)? = null
) {
    var currentSelection by remember { mutableStateOf(selectedOptions) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    
    val columnCount = when {
        screenWidth >= 900 -> if (options.size > 8) 4 else 3
        screenWidth >= 600 -> if (options.size > 6) 3 else 2
        else -> if (options.size > 4) 2 else 1
    }

    val dialogWidthFraction = when {
        screenWidth >= 900 -> 0.65f
        screenWidth >= 600 -> 0.75f
        else -> 0.90f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth(dialogWidthFraction)
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1B1D22).copy(alpha = 0.98f),
                                Color(0xFF101216).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(1.dp, Color.Cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                        color = Color.Cyan.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (screenHeight * 0.6f).dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options) { option ->
                            PlayerOptionRow(
                                label = option,
                                isSelected = currentSelection.contains(option),
                                isRadioStyle = singleSelect,
                                onToggle = { selected ->
                                    if (singleSelect) {
                                        currentSelection = if (selected) setOf(option) else emptySet()
                                    } else {
                                        currentSelection = if (selected) currentSelection + option else currentSelection - option
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                        color = Color.Cyan.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onReset != null) {
                            PlayerDialogActionBtn(text = "Reset", baseColor = Color(0xFFFF5252), onClick = onReset)
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        PlayerDialogActionBtn(text = "Cancel", baseColor = Color.Cyan, isFilled = false, onClick = onDismiss)
                        Spacer(modifier = Modifier.width(10.dp))
                        PlayerDialogActionBtn(text = "Apply", baseColor = Color.Cyan, isFilled = true, onClick = { onConfirm(currentSelection) })
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerOptionRow(
    label: String,
    isSelected: Boolean,
    isRadioStyle: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, animationSpec = tween(150), label = "")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onToggle(!isSelected) },
        shape = RoundedCornerShape(8.dp),
        color = when {
            isFocused -> Color.Cyan.copy(alpha = 0.25f)
            isSelected -> Color.Cyan.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> Color.Cyan
                isSelected -> Color.Cyan.copy(alpha = 0.6f)
                else -> Color.Transparent
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(if (isRadioStyle) CircleShape else RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color.Cyan else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) Color.Cyan else if (isFocused) Color.White else Color.Gray.copy(alpha = 0.5f),
                        if (isRadioStyle) CircleShape else RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected && !isRadioStyle) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                } else if (isSelected && isRadioStyle) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isFocused || isSelected) Color.Cyan else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlayerDialogActionBtn(
    text: String,
    baseColor: Color,
    onClick: () -> Unit,
    isFilled: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, animationSpec = tween(120), label = "")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFilled && isFocused) baseColor.copy(alpha = 0.85f) else if (isFilled) baseColor else if (isFocused) baseColor.copy(alpha = 0.18f) else Color.Transparent)
            .border(if (isFocused) 1.5.dp else 1.dp, if (isFocused || !isFilled) baseColor else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFilled && isFocused) Color.White else if (isFilled) Color.Black else if (isFocused) baseColor else Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}