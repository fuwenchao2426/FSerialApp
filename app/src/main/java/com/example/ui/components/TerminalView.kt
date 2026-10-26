package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalLine
import com.example.ui.theme.*

@Composable
fun TerminalView(
    lines: List<TerminalLine>,
    isHexDisplay: Boolean,
    showTimestamps: Boolean,
    showLineNumbers: Boolean,
    isPaused: Boolean,
    onToggleHex: () -> Unit,
    onToggleTimestamps: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    onTogglePause: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, isPaused) {
        if (!isPaused && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardSurface)
            .testTag("terminal_panel")
    ) {
        // Terminal Top Action Toolbar
        Surface(
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = isHexDisplay,
                        onClick = onToggleHex,
                        label = { Text("HEX", fontSize = 11.sp) },
                        modifier = Modifier.height(26.dp)
                    )

                    FilterChip(
                        selected = showTimestamps,
                        onClick = onToggleTimestamps,
                        label = { Text("时间戳", fontSize = 11.sp) },
                        modifier = Modifier.height(26.dp)
                    )

                    FilterChip(
                        selected = showLineNumbers,
                        onClick = onToggleLineNumbers,
                        label = { Text("行号", fontSize = 11.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "继续滚动" else "暂停滚动",
                            tint = if (isPaused) StatusAmber else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "清空接收区",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Terminal Log List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无串口报文，连接后将实时输出...",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lines, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (showLineNumbers) {
                                Text(
                                    text = "%04d ".format(item.id % 10000),
                                    color = Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (showTimestamps) {
                                Text(
                                    text = "${item.timestamp} ",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = if (item.isTx) "[TX] " else "[RX] ",
                                color = if (item.isTx) StatusAmber else StatusGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = if (isHexDisplay) item.hexText else item.text,
                                color = Color(0xFFECEFF1),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (isPaused) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusAmber.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "已冻结滚屏 (数据继续接收)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
