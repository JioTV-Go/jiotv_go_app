package com.skylake.skytv.jgorunner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun OmniFilterDialog(
    title: String,
    options: List<String>,
    selectedOptions: Set<String>,
    singleSelect: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onReset: (() -> Unit)? = null
) {
    var currentSelection by remember { mutableStateOf(selectedOptions) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222),
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f).border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Cyan,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(options) { option ->
                        FilterItemRow(
                            label = option,
                            isSelected = currentSelection.contains(option),
                            onToggle = { selected ->
                                if (singleSelect) {
                                    if (selected) {
                                        onConfirm(setOf(option))
                                    } else {
                                        onConfirm(emptySet())
                                    }
                                } else {
                                    currentSelection = if (selected) {
                                        currentSelection + option
                                    } else {
                                        currentSelection - option
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onReset != null) {
                        var isResetFocused by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier
                                .onFocusChanged { isResetFocused = it.isFocused }
                                .background(if (isResetFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .border(1.dp, if (isResetFocused) Color.Red else Color.Transparent, RoundedCornerShape(4.dp))
                        ) {
                            Text("Reset", color = Color.Red)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    var isCancelFocused by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .onFocusChanged { isCancelFocused = it.isFocused }
                            .background(if (isCancelFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(4.dp))
                            .border(1.dp, if (isCancelFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(4.dp))
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                    if (!singleSelect) {
                        Spacer(modifier = Modifier.width(16.dp))
                        var isApplyFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { onConfirm(currentSelection) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isApplyFocused) Color.White else Color.Cyan),
                            modifier = Modifier
                                .onFocusChanged { isApplyFocused = it.isFocused }
                                .border(1.dp, if (isApplyFocused) Color.Cyan else Color.Transparent, ButtonDefaults.shape)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterItemRow(
    label: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (isFocused) Color.Cyan.copy(alpha = 0.1f) else Color.Transparent)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (isFocused) Color.Cyan else Color.Transparent, RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Cyan,
                uncheckedColor = if (isFocused) Color.White else Color.Gray
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isFocused) Color.Cyan else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
