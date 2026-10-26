package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.serial.SerialConfig
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsDialog(
    currentConfig: SerialConfig,
    onSave: (dataBits: Int, stopBits: Int, parity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var dataBits by remember { mutableIntStateOf(currentConfig.dataBits) }
    var stopBits by remember { mutableIntStateOf(currentConfig.stopBits) }
    var parity by remember { mutableIntStateOf(currentConfig.parity) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("串口高级参数设置 (8-N-1)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            // Data bits
            Text("数据位 (Data Bits):", fontSize = 12.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                listOf(8, 7, 6, 5).forEach { b ->
                    FilterChip(
                        selected = dataBits == b,
                        onClick = { dataBits = b },
                        label = { Text("$b 位") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stop bits
            Text("停止位 (Stop Bits):", fontSize = 12.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                listOf(1, 2).forEach { s ->
                    FilterChip(
                        selected = stopBits == s,
                        onClick = { stopBits = s },
                        label = { Text("$s 位") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parity
            Text("校验位 (Parity):", fontSize = 12.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                listOf(0 to "None (无)", 1 to "Odd (奇)", 2 to "Even (偶)").forEach { (p, l) ->
                    FilterChip(
                        selected = parity == p,
                        onClick = { parity = p },
                        label = { Text(l) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(dataBits, stopBits, parity)
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
