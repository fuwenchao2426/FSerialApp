package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.formula.FormulaEvaluator
import com.example.protocol.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameEditorDialog(
    initialConfig: ProtocolConfig,
    onSave: (ProtocolConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var isHexMode by remember { mutableStateOf(initialConfig.isHexMode) }
    var parseMode by remember { mutableStateOf(initialConfig.textParseMode) }
    var frameHeader by remember { mutableStateOf(initialConfig.frameHeader) }
    var frameTail by remember { mutableStateOf(initialConfig.frameTail) }
    var delimiter by remember { mutableStateOf(initialConfig.delimiter) }
    var textFields by remember { mutableStateOf(initialConfig.textFields) }

    var hexFields by remember { mutableStateOf(initialConfig.hexFields) }
    var checksumType by remember { mutableStateOf(initialConfig.checksumType) }
    var carryMode by remember { mutableStateOf(initialConfig.carryMode) }
    var invertMode by remember { mutableStateOf(initialConfig.invertMode) }

    var txTemplate by remember { mutableStateOf(initialConfig.txTemplate) }
    var autoSendOnParamChange by remember { mutableStateOf(initialConfig.autoSendOnParamChange) }

    var testRawVal by remember { mutableStateOf("16384") }
    var testFormula by remember { mutableStateOf("[x]*2000/32768") }
    val testResult = remember(testRawVal, testFormula) {
        val raw = testRawVal.toDoubleOrNull() ?: 0.0
        FormulaEvaluator.evaluate(testFormula, raw)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("frame_editor_dialog"),
            shape = RoundedCornerShape(12.dp),
            color = CardSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DataObject, null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "协议解析与公式转换设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                        Button(
                            onClick = {
                                val updated = initialConfig.copy(
                                    isHexMode = isHexMode,
                                    textParseMode = parseMode,
                                    frameHeader = frameHeader,
                                    frameTail = frameTail,
                                    delimiter = delimiter,
                                    textFields = textFields,
                                    hexFields = hexFields,
                                    checksumType = checksumType,
                                    carryMode = carryMode,
                                    invertMode = invertMode,
                                    txTemplate = txTemplate,
                                    autoSendOnParamChange = autoSendOnParamChange
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("保存生效")
                        }
                    }
                }

                // Tab Selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; isHexMode = false },
                        text = { Text("1. 文本/CSV解析") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; isHexMode = true },
                        text = { Text("2. HEX二进制解析") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("3. TX参数下发模板") }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> TextCsvEditorPane(
                            parseMode = parseMode,
                            onParseModeChange = { parseMode = it },
                            frameHeader = frameHeader,
                            onHeaderChange = { frameHeader = it },
                            frameTail = frameTail,
                            onTailChange = { frameTail = it },
                            delimiter = delimiter,
                            onDelimiterChange = { delimiter = it },
                            fields = textFields,
                            onFieldsChange = { textFields = it },
                            testRawVal = testRawVal,
                            onTestRawChange = { testRawVal = it },
                            testFormula = testFormula,
                            onTestFormulaChange = { testFormula = it },
                            testResult = testResult
                        )
                        1 -> HexEditorPane(
                            fields = hexFields,
                            onFieldsChange = { hexFields = it },
                            checksumType = checksumType,
                            onChecksumTypeChange = { checksumType = it },
                            carryMode = carryMode,
                            onCarryModeChange = { carryMode = it },
                            invertMode = invertMode,
                            onInvertModeChange = { invertMode = it }
                        )
                        2 -> TxTemplatePane(
                            template = txTemplate,
                            onTemplateChange = { txTemplate = it },
                            autoSend = autoSendOnParamChange,
                            onAutoSendChange = { autoSendOnParamChange = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextCsvEditorPane(
    parseMode: TextParseMode,
    onParseModeChange: (TextParseMode) -> Unit,
    frameHeader: String,
    onHeaderChange: (String) -> Unit,
    frameTail: String,
    onTailChange: (String) -> Unit,
    delimiter: String,
    onDelimiterChange: (String) -> Unit,
    fields: List<FrameField>,
    onFieldsChange: (List<FrameField>) -> Unit,
    testRawVal: String,
    onTestRawChange: (String) -> Unit,
    testFormula: String,
    onTestFormulaChange: (String) -> Unit,
    testResult: Double
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = parseMode == TextParseMode.DELIMITER,
                        onClick = { onParseModeChange(TextParseMode.DELIMITER) }
                    )
                    Text("分隔模式 (逗号/空格分隔)", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = parseMode == TextParseMode.MATCH,
                        onClick = { onParseModeChange(TextParseMode.MATCH) }
                    )
                    Text("匹配模式 (@变量名)", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = frameHeader,
                    onValueChange = onHeaderChange,
                    label = { Text("帧头", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = frameTail,
                    onValueChange = onTailChange,
                    label = { Text("帧尾", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = delimiter,
                    onValueChange = onDelimiterChange,
                    label = { Text("分隔符", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("🧪 公式在线测试器 (支持 [x], if, 四则/位运算, Abs, Sin, Round 等)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = testRawVal,
                            onValueChange = onTestRawChange,
                            label = { Text("原始[x]", fontSize = 10.sp) },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = testFormula,
                            onValueChange = onTestFormulaChange,
                            label = { Text("计算公式", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(
                            text = "= %.2f".format(testResult),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = StatusGreen
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("数据字段与公式映射列表 (${fields.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Button(
                    onClick = {
                        val newId = fields.size
                        val newList = fields.toMutableList()
                        newList.add(FrameField(newId, "CH${newId + 1}", formula = "[x]"))
                        onFieldsChange(newList)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("+ 添加字段", fontSize = 11.sp)
                }
            }
        }

        itemsIndexed(fields) { idx, field ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#$idx", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = field.name,
                        onValueChange = { newName ->
                            val updated = fields.toMutableList()
                            updated[idx] = field.copy(name = newName)
                            onFieldsChange(updated)
                        },
                        label = { Text("命名", fontSize = 9.sp) },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = field.formula,
                        onValueChange = { newForm ->
                            val updated = fields.toMutableList()
                            updated[idx] = field.copy(formula = newForm)
                            onFieldsChange(updated)
                        },
                        label = { Text("公式", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = field.unit,
                        onValueChange = { newUnit ->
                            val updated = fields.toMutableList()
                            updated[idx] = field.copy(unit = newUnit)
                            onFieldsChange(updated)
                        },
                        label = { Text("单位", fontSize = 9.sp) },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val updated = fields.toMutableList()
                            updated.removeAt(idx)
                            onFieldsChange(updated)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HexEditorPane(
    fields: List<HexField>,
    onFieldsChange: (List<HexField>) -> Unit,
    checksumType: ChecksumType,
    onChecksumTypeChange: (ChecksumType) -> Unit,
    carryMode: CarryMode,
    onCarryModeChange: (CarryMode) -> Unit,
    invertMode: InvertMode,
    onInvertModeChange: (InvertMode) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("添加字段 (点击按钮向末尾添加字段积木):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    HexBlockType.HEADER to "帧头",
                    HexBlockType.ID to "帧ID",
                    HexBlockType.LEN to "长度",
                    HexBlockType.BYTE1 to "1Byte",
                    HexBlockType.BYTE2 to "2Byte",
                    HexBlockType.BYTE4 to "4Byte",
                    HexBlockType.CHECKSUM to "校验",
                    HexBlockType.TAIL to "帧尾"
                ).forEach { (bType, label) ->
                    AssistChip(
                        onClick = {
                            val nextId = fields.size
                            val defaultHex = when (bType) {
                                HexBlockType.HEADER -> "FF AA"
                                HexBlockType.TAIL -> "AA"
                                HexBlockType.BYTE2 -> "00 00"
                                HexBlockType.BYTE4 -> "00 00 00 00"
                                else -> "00"
                            }
                            val updated = fields.toMutableList()
                            updated.add(HexField(nextId, label, bType, defaultHex))
                            onFieldsChange(updated)
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Text("校验算法设置 (Checksum Properties):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = checksumType == ChecksumType.SUM8,
                    onClick = { onChecksumTypeChange(ChecksumType.SUM8) },
                    label = { Text("SUM8", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = checksumType == ChecksumType.CRC16_MODBUS,
                    onClick = { onChecksumTypeChange(ChecksumType.CRC16_MODBUS) },
                    label = { Text("CRC16", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = checksumType == ChecksumType.XOR8,
                    onClick = { onChecksumTypeChange(ChecksumType.XOR8) },
                    label = { Text("XOR8", fontSize = 11.sp) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = carryMode == CarryMode.DROP,
                        onClick = { onCarryModeChange(CarryMode.DROP) }
                    )
                    Text("丢弃进位", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = carryMode == CarryMode.ACCUMULATE,
                        onClick = { onCarryModeChange(CarryMode.ACCUMULATE) }
                    )
                    Text("进位循环累加", fontSize = 11.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text("HEX 字段排列列表:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        itemsIndexed(fields) { idx, field ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#$idx", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrimaryBlueContainer
                    ) {
                        Text(
                            text = field.blockType.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    OutlinedTextField(
                        value = field.name,
                        onValueChange = { newN ->
                            val updated = fields.toMutableList()
                            updated[idx] = field.copy(name = newN)
                            onFieldsChange(updated)
                        },
                        label = { Text("名称", fontSize = 9.sp) },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = field.defaultHex,
                        onValueChange = { newH ->
                            val updated = fields.toMutableList()
                            updated[idx] = field.copy(defaultHex = newH)
                            onFieldsChange(updated)
                        },
                        label = { Text("默认HEX", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val updated = fields.toMutableList()
                            updated.removeAt(idx)
                            onFieldsChange(updated)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TxTemplatePane(
    template: String,
    onTemplateChange: (String) -> Unit,
    autoSend: Boolean,
    onAutoSendChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("TX 动态参数发送帧模板 (TX Template):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            text = "格式：SET,@target_speed:int^3000,@kp:float^10.0\\r\\n\n系统会在【动态组件】栏自动生成这些参数的滑块和步进器！",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        OutlinedTextField(
            value = template,
            onValueChange = onTemplateChange,
            label = { Text("发送指令模板") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("参数变动自动发送 (Auto-Send with Debounce)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text("滑动滑块或修改数值 150ms 后自动组装下发", fontSize = 10.sp, color = TextMuted)
            }
            Switch(
                checked = autoSend,
                onCheckedChange = onAutoSendChange
            )
        }
    }
}
