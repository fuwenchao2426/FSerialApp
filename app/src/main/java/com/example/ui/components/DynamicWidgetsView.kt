package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DynamicWidgetData
import com.example.ui.theme.*

@Composable
fun DynamicWidgetsView(
    widgets: List<DynamicWidgetData>,
    txParams: Map<String, Double>,
    autoSendEnabled: Boolean,
    onTxParamChanged: (String, Double) -> Unit,
    onSendSingleTxParam: () -> Unit,
    onOpenFrameEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileColors = listOf(TileSky, TileTeal, TilePurple, TileAmber, TileRose, TileIndigo)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .padding(8.dp)
            .testTag("dynamic_widgets_panel")
    ) {
        // Section 1 Header: TX Parameter Tuning Bar (JCOM/Airplane style)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TX 动态调参控制器", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (autoSendEnabled) "变动自动发送" else "手动单次下发",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (autoSendEnabled) StatusGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onSendSingleTxParam,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下发指令", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive sliders & steppers for txParams
                txParams.forEach { (key, value) ->
                    val maxVal = if (key.contains("speed")) 3000.0 else 20.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(90.dp)
                        )

                        // Stepper -
                        IconButton(
                            onClick = {
                                val step = if (maxVal > 100) 50.0 else 0.5
                                onTxParamChanged(key, (value - step).coerceAtLeast(0.0))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }

                        Slider(
                            value = value.toFloat(),
                            onValueChange = { onTxParamChanged(key, it.toDouble()) },
                            valueRange = 0f..maxVal.toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue
                            )
                        )

                        // Stepper +
                        IconButton(
                            onClick = {
                                val step = if (maxVal > 100) 50.0 else 0.5
                                onTxParamChanged(key, (value + step).coerceAtMost(maxVal))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = if (value % 1.0 == 0.0) "%.0f".format(value) else "%.2f".format(value),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .width(50.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 2 Header: RX Data Display Panels (JCOM blue/teal tiles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dashboard, null, tint = TileSky, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RX 数据展示面板 (JCOM 格式)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            }

            TextButton(
                onClick = onOpenFrameEditor,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("配置协议/公式", fontSize = 11.sp)
            }
        }

        if (widgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "等待串口接收数据流，根据协议格式自动生成磁贴...",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(widgets) { item ->
                    val colorIndex = Math.abs(item.name.hashCode()) % tileColors.size
                    val tileColor = tileColors[colorIndex]

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = tileColor,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = if (item.formula != "[x]") "fx" else "raw",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "%.2f".format(item.scaledValue),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = if (item.unit.isNotEmpty()) item.unit else item.formula,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val progress = if (item.maxVal > item.minVal) {
                                ((item.scaledValue - item.minVal) / (item.maxVal - item.minVal)).toFloat().coerceIn(0f, 1f)
                            } else 0.5f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }
}
