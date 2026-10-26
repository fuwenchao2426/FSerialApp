package com.example.protocol

enum class TextParseMode {
    DELIMITER, // 分隔模式 (每行作为一帧，指定分隔符)
    MATCH      // 匹配模式 (指定前置匹配文本)
}

enum class HexBlockType(val label: String, val byteLength: Int) {
    HEADER("帧头", 2),
    SEQ("帧序号", 1),
    ID("帧ID", 1),
    LEN("帧长度", 1),
    BYTE1("1Byte", 1),
    BYTE2("2Byte", 2),
    BYTE3("3Byte", 3),
    BYTE4("4Byte", 4),
    BYTE8("8Byte", 8),
    ARRAY_FIXED("定长数组", 4),
    ARRAY_VAR("变长数组", 0),
    CHECKSUM("校验", 1),
    TAIL("帧尾", 1)
}

enum class ChecksumType(val label: String) {
    SUM8("SUM8 (单字节累加和)"),
    SUM16("SUM16 (双字节累加和)"),
    CRC16_MODBUS("CRC16-Modbus"),
    XOR8("XOR8 (单字节异或)")
}

enum class CarryMode {
    DROP,       // 丢弃进位
    ACCUMULATE  // 进位循环累加
}

enum class InvertMode {
    NONE,             // 正常
    INVERT,           // 结果取反
    INVERT_PLUS_ONE   // 结果取反+1 (补码)
}

data class FrameField(
    val id: Int,
    val name: String,
    val isNumeric: Boolean = true,
    val showInPanel: Boolean = true,
    val formula: String = "[x]",
    val unit: String = "",
    val minVal: Double = 0.0,
    val maxVal: Double = 1000.0,
    val isTxParam: Boolean = false,
    val step: Double = 1.0
)

data class HexField(
    val id: Int,
    val name: String,
    val blockType: HexBlockType,
    val defaultHex: String = "00",
    val showInPanel: Boolean = true,
    val formula: String = "[x]"
)

data class ProtocolConfig(
    val isHexMode: Boolean = false,
    val textParseMode: TextParseMode = TextParseMode.DELIMITER,
    val frameHeader: String = "KS",
    val frameTail: String = "\\r\\n",
    val delimiter: String = ",",
    val textFields: List<FrameField> = listOf(
        FrameField(0, "f27", isNumeric = true, showInPanel = true, formula = "[x]", unit = "rpm", maxVal = 5000.0),
        FrameField(1, "43dt", isNumeric = true, showInPanel = true, formula = "[x]*2000/32768", unit = "°/s", maxVal = 2000.0),
        FrameField(2, "uv5", isNumeric = true, showInPanel = true, formula = "[x]*3.3/4096", unit = "V", maxVal = 3.3),
        FrameField(3, "key", isNumeric = true, showInPanel = true, formula = "if([x]>1000, 1, 0)", unit = "state", maxVal = 1.0)
    ),
    val hexFields: List<HexField> = listOf(
        HexField(0, "帧头", HexBlockType.HEADER, "FF AA"),
        HexField(1, "帧ID", HexBlockType.ID, "01"),
        HexField(2, "帧长度", HexBlockType.LEN, "04"),
        HexField(3, "2Byte", HexBlockType.BYTE2, "00 00", showInPanel = true, formula = "[x]"),
        HexField(4, "2Byte", HexBlockType.BYTE2, "00 00", showInPanel = true, formula = "[x]*0.1"),
        HexField(5, "校验", HexBlockType.CHECKSUM, "00"),
        HexField(6, "帧尾", HexBlockType.TAIL, "AA")
    ),
    val checksumStartIdx: Int = 0,
    val checksumEndIdx: Int = 4,
    val checksumType: ChecksumType = ChecksumType.SUM8,
    val carryMode: CarryMode = CarryMode.DROP,
    val invertMode: InvertMode = InvertMode.NONE,
    val txTemplate: String = "SET,@target_speed:int^3000,@kp:float^10.0\\r\\n",
    val autoSendOnParamChange: Boolean = false
)

data class ParsedChannelValue(
    val channelIndex: Int,
    val name: String,
    val rawValue: Double,
    val scaledValue: Double,
    val unit: String
)
