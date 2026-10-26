package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PresetCommand
import com.example.ui.theme.*

@Composable
fun SendBar(
    sendText: String,
    isHexSend: Boolean,
    presets: List<PresetCommand>,
    onSendTextChanged: (String) -> Unit,
    onToggleHexSend: () -> Unit,
    onSend: () -> Unit,
    onSendPreset: (PresetCommand) -> Unit,
    onOpenHistory: () -> Unit,
    onAddPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Preset Commands Scrollable Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(presets) { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.clickable { onSendPreset(preset) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Add Preset Button
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryBlueContainer,
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { onAddPreset() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, "添加预设", tint = PrimaryBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "添加预设",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Input & Send Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // HEX / Text toggle pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isHexSend) PrimaryBlue else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isHexSend) PrimaryBlue else BorderSubtle),
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { onToggleHexSend() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isHexSend) "HEX" else "文本",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHexSend) Color.White else TextPrimary
                        )
                    }
                }

                // Text Input Field with Clear & History dropdown
                OutlinedTextField(
                    value = sendText,
                    onValueChange = onSendTextChanged,
                    placeholder = {
                        Text(
                            text = if (isHexSend) "输入十六进制 (如 01 02 A0)..." else "输入发送指令...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sendText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSendTextChanged("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "清空", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = onOpenHistory,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "发送历史",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )

                // Send Button
                Button(
                    onClick = onSend,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("发送", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
