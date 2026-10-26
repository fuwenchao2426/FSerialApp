package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    historyList: List<HistoryItem>,
    onResendItem: (HistoryItem) -> Unit,
    onFillInput: (HistoryItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface,
        modifier = Modifier.testTag("history_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "发送历史记录 (${historyList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (historyList.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Icon(Icons.Default.DeleteSweep, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清空历史", color = StatusRed, fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "点击条目回显编辑，点击右侧小火箭直接一键秒发",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider()

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无发送历史记录", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFillInput(item) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mode badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.isHex) TilePurple.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (item.isHex) "HEX" else "TXT",
                                        color = if (item.isHex) TilePurple else PrimaryBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Command content & timestamp
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.commandText,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = item.timestamp,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                // Quick 1-click Resend button (🚀)
                                IconButton(
                                    onClick = { onResendItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "一键重发",
                                        tint = StatusGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete single item button
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除本条",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
