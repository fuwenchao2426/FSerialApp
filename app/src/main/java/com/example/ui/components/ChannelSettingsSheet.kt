package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oscilloscope.ScopeChannelState
import com.example.oscilloscope.TRACE_COLORS
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsSheet(
    channel: ScopeChannelState,
    onSave: (name: String, color: Color, colorHex: String, windowIdx: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(channel.name) }
    var selectedColor by remember { mutableStateOf(channel.color) }
    var selectedColorHex by remember { mutableStateOf(channel.colorHex) }
    var windowIdx by remember { mutableIntStateOf(channel.windowIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("配置通道 #${channel.id + 1} 属性", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("通道名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("分配显示视窗:", fontSize = 12.sp, color = TextSecondary)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = windowIdx == 0,
                    onClick = { windowIdx = 0 },
                    label = { Text("视窗 1 (主控/速度)") }
                )
                FilterChip(
                    selected = windowIdx == 1,
                    onClick = { windowIdx = 1 },
                    label = { Text("视窗 2 (电压/辅助)") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("曲线高亮色彩 (20色工业色系):", fontSize = 12.sp, color = TextSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TRACE_COLORS.take(10).forEach { (col, hex) ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                2.dp,
                                if (selectedColorHex == hex) TextPrimary else Color.Transparent,
                                CircleShape
                            )
                            .clickable {
                                selectedColor = col
                                selectedColorHex = hex
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(name, selectedColor, selectedColorHex, windowIdx)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("保存设置")
            }
        }
    }
}
