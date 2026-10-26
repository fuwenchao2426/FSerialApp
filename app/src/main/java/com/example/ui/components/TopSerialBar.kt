package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.serial.SerialConfig
import com.example.serial.SerialStatus
import com.example.serial.SerialTrafficStats
import com.example.ui.theme.*

@Composable
fun TopSerialBar(
    serialStatus: SerialStatus,
    serialConfig: SerialConfig,
    trafficStats: SerialTrafficStats,
    availablePorts: List<String>,
    onToggleConnect: () -> Unit,
    onPortSelected: (String) -> Unit,
    onBaudSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFrameEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var portDropdownOpen by remember { mutableStateOf(false) }
    var baudDropdownOpen by remember { mutableStateOf(false) }

    val isConnected = serialStatus is SerialStatus.Connected
    val isConnecting = serialStatus is SerialStatus.Connecting

    val statusTextCol by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF047857) else TextSecondary,
        label = "statusText"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ledPulse")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected) 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_serial_bar"),
        color = CardSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Port Selector Dropdown Button
                Box(modifier = Modifier.weight(1.3f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { portDropdownOpen = true }
                            .testTag("port_dropdown_btn"),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (serialConfig.isVirtual) Icons.Default.Sensors else Icons.Default.Usb,
                                contentDescription = null,
                                tint = if (serialConfig.isVirtual) TilePurple else PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = serialConfig.portName.substringBefore(" ("),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = portDropdownOpen,
                        onDismissRequest = { portDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚡ 内置仿真信号流 (演示/测试)") },
                            onClick = {
                                onPortSelected("Virtual Demo (内置仿真流)")
                                portDropdownOpen = false
                            },
                            leadingIcon = { Icon(Icons.Default.Sensors, null, tint = TilePurple) }
                        )
                        HorizontalDivider()
                        if (availablePorts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("未检测到 USB 硬件 (可插OTG或用仿真)", color = TextMuted) },
                                onClick = { portDropdownOpen = false },
                                enabled = false
                            )
                        } else {
                            availablePorts.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p, fontSize = 13.sp) },
                                    onClick = {
                                        onPortSelected(p)
                                        portDropdownOpen = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Usb, null, tint = PrimaryBlue) }
                                )
                            }
                        }
                    }
                }

                // Baud Rate Selector Dropdown Button
                val baudRates = listOf(9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600)
                Box(modifier = Modifier.weight(0.9f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { baudDropdownOpen = true }
                            .testTag("baud_dropdown_btn"),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${serialConfig.baudRate}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = baudDropdownOpen,
                        onDismissRequest = { baudDropdownOpen = false }
                    ) {
                        baudRates.forEach { rate ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "$rate",
                                        fontWeight = if (rate == serialConfig.baudRate) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                onClick = {
                                    onBaudSelected(rate)
                                    baudDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Connect / Disconnect Action Button (Green / Red)
                Button(
                    onClick = onToggleConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) StatusRed else StatusGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("connect_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isConnected) ledAlpha else 1f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "关闭" else if (isConnecting) "连接中" else "打开",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Frame Protocol Editor Button
                IconButton(
                    onClick = onOpenFrameEditor,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("frame_editor_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = "帧格式设置",
                        tint = TileSky
                    )
                }

                // Settings Gear Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "串口设置",
                        tint = TextSecondary
                    )
                }
            }

            // Real-time Traffic Counters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) StatusGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isConnected) "已连通" else "已断开",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusTextCol
                        )
                    }

                    Text(
                        text = "RX: ${trafficStats.rxBytes} B",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StatusGreen
                    )
                    Text(
                        text = "TX: ${trafficStats.txBytes} B",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StatusAmber
                    )
                    Text(
                        text = "帧: ${trafficStats.frameCount}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Text(
                    text = "${serialConfig.dataBits}-N-${serialConfig.stopBits}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}
