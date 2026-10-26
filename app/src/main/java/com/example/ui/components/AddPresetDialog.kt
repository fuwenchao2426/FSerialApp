package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.SuffixType

@Composable
fun AddPresetDialog(
    onAdd: (label: String, cmdText: String, isHex: Boolean, suffix: SuffixType) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var cmdText by remember { mutableStateOf("") }
    var isHex by remember { mutableStateOf(false) }
    var suffix by remember { mutableStateOf(SuffixType.CR_LF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建常用快捷指令") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("按钮标签 (如 复位/START)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cmdText,
                    onValueChange = { cmdText = it },
                    label = { Text("指令内容 (如 AT+RST 或 01 02)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(
                        selected = isHex,
                        onClick = { isHex = !isHex },
                        label = { Text(if (isHex) "HEX格式" else "文本格式") }
                    )
                    FilterChip(
                        selected = suffix == SuffixType.CR_LF,
                        onClick = { suffix = if (suffix == SuffixType.CR_LF) SuffixType.NONE else SuffixType.CR_LF },
                        label = { Text("追加\\r\\n") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && cmdText.isNotBlank()) {
                        onAdd(label.trim(), cmdText.trim(), isHex, suffix)
                        onDismiss()
                    }
                },
                enabled = label.isNotBlank() && cmdText.isNotBlank()
            ) {
                Text("添加保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
