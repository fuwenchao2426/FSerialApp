package com.example.protocol

import com.example.formula.FormulaEvaluator
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProtocolParser(private var config: ProtocolConfig = ProtocolConfig()) {

    private val textBuffer = StringBuilder()

    fun updateConfig(newConfig: ProtocolConfig) {
        config = newConfig
    }

    /**
     * Parses incoming text data and yields parsed numerical channels for oscilloscope & widgets.
     */
    fun parseIncomingText(text: String): List<List<ParsedChannelValue>> {
        val result = mutableListOf<List<ParsedChannelValue>>()
        textBuffer.append(text)

        while (true) {
            val newlineIdx = textBuffer.indexOfAny(charArrayOf('\n', '\r'))
            if (newlineIdx == -1) break

            val line = textBuffer.substring(0, newlineIdx).trim()
            // Skip over consecutive newlines
            var skipCount = 1
            while (newlineIdx + skipCount < textBuffer.length &&
                (textBuffer[newlineIdx + skipCount] == '\n' || textBuffer[newlineIdx + skipCount] == '\r')
            ) {
                skipCount++
            }
            textBuffer.delete(0, newlineIdx + skipCount)

            if (line.isNotEmpty()) {
                val parsedRow = parseLine(line)
                if (parsedRow.isNotEmpty()) {
                    result.add(parsedRow)
                }
            }
        }

        // Prevent buffer bloat if no newline is received for a long time
        if (textBuffer.length > 8192) {
            textBuffer.delete(0, textBuffer.length - 2048)
        }

        return result
    }

    private fun parseLine(line: String): List<ParsedChannelValue> {
        val row = mutableListOf<ParsedChannelValue>()

        // Check if line contains inline @param:type^max format
        if (line.contains("@")) {
            val inlineValues = parseInlineParamTags(line)
            if (inlineValues.isNotEmpty()) return inlineValues
        }

        var content = line
        val header = config.frameHeader.trim()
        if (header.isNotEmpty()) {
            if (content.startsWith(header)) {
                content = content.substring(header.length).trimStart(',', ' ', ':', '=')
            } else if (!line.contains(",")) {
                // If header doesn't match and line isn't plain CSV, return empty
                return emptyList()
            }
        }

        val delimiter = if (config.delimiter.isEmpty()) "," else config.delimiter
        val tokens = content.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }

        val fields = config.textFields
        for (i in tokens.indices) {
            val token = tokens[i]
            val raw = token.toDoubleOrNull() ?: continue
            val field = fields.getOrNull(i) ?: FrameField(i, "CH$i", formula = "[x]")

            val scaled = FormulaEvaluator.evaluate(field.formula, raw)
            row.add(
                ParsedChannelValue(
                    channelIndex = i,
                    name = field.name,
                    rawValue = raw,
                    scaledValue = scaled,
                    unit = field.unit
                )
            )
        }

        return row
    }

    private fun parseInlineParamTags(line: String): List<ParsedChannelValue> {
        // Matches e.g. @speed:1250 or @speed:int^3000:1250 or Go,@speed:int^3000=1500
        val pattern = Regex("""@([a-zA-Z0-9_]+)(?::[a-zA-Z0-9_^]+)?(?:[:=])([+-]?\d+(?:\.\d+)?)""")
        val matches = pattern.findAll(line)
        val row = mutableListOf<ParsedChannelValue>()
        var idx = 0

        for (m in matches) {
            val name = m.groupValues[1]
            val valStr = m.groupValues[2]
            val raw = valStr.toDoubleOrNull() ?: continue
            val formula = config.textFields.find { it.name == name }?.formula ?: "[x]"
            val scaled = FormulaEvaluator.evaluate(formula, raw)

            row.add(
                ParsedChannelValue(
                    channelIndex = idx++,
                    name = name,
                    rawValue = raw,
                    scaledValue = scaled,
                    unit = config.textFields.find { it.name == name }?.unit ?: ""
                )
            )
        }

        return row
    }

    /**
     * Parses binary HEX stream bytes.
     */
    fun parseIncomingBytes(bytes: ByteArray): List<List<ParsedChannelValue>> {
        val result = mutableListOf<List<ParsedChannelValue>>()
        if (bytes.size < 4) return result

        val headerHex = config.hexFields.firstOrNull { it.blockType == HexBlockType.HEADER }?.defaultHex ?: "FFAA"
        val headerBytes = hexStringToByteArray(headerHex)

        var i = 0
        while (i <= bytes.size - headerBytes.size) {
            var match = true
            for (h in headerBytes.indices) {
                if (bytes[i + h] != headerBytes[h]) {
                    match = false
                    break
                }
            }

            if (match) {
                val row = mutableListOf<ParsedChannelValue>()
                var offset = i + headerBytes.size
                var chIdx = 0

                for (field in config.hexFields) {
                    if (field.blockType == HexBlockType.HEADER || field.blockType == HexBlockType.TAIL || field.blockType == HexBlockType.CHECKSUM) {
                        continue
                    }
                    val bLen = field.blockType.byteLength
                    if (offset + bLen <= bytes.size) {
                        val rawVal = when (bLen) {
                            1 -> (bytes[offset].toInt() and 0xFF).toDouble()
                            2 -> ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
                            4 -> ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toDouble()
                            else -> 0.0
                        }
                        val scaled = FormulaEvaluator.evaluate(field.formula, rawVal)
                        row.add(
                            ParsedChannelValue(
                                channelIndex = chIdx++,
                                name = field.name,
                                rawValue = rawVal,
                                scaledValue = scaled,
                                unit = ""
                            )
                        )
                        offset += bLen
                    }
                }
                if (row.isNotEmpty()) {
                    result.add(row)
                }
                i = offset
            } else {
                i++
            }
        }

        return result
    }

    /**
     * Formats TX parameters into the configured transmission template string.
     */
    fun formatTxTemplate(params: Map<String, Double>): String {
        var template = config.txTemplate
        // Replace unescaped \r and \n with actual characters
        template = template.replace("\\r", "\r").replace("\\n", "\n")

        for ((key, value) in params) {
            val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
            // Replace e.g. @key:int^max or @key
            val regex = Regex("""@${Regex.escape(key)}(?::[a-zA-Z0-9_^]+)?""")
            template = regex.replace(template, formatted)
        }
        return template
    }

    companion object {
        fun hexStringToByteArray(s: String): ByteArray {
            val clean = s.replace(" ", "").replace("0x", "", ignoreCase = true)
            val len = clean.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len - 1) {
                data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        fun byteArrayToHexString(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): String {
            val sb = StringBuilder()
            val end = (offset + length).coerceAtMost(bytes.size)
            for (i in offset until end) {
                sb.append("%02X ".format(bytes[i]))
            }
            return sb.toString().trimEnd()
        }

        /**
         * Calculates CRC16-Modbus for given byte slice.
         */
        fun calculateCrc16Modbus(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
            var crc = 0xFFFF
            val end = (offset + length).coerceAtMost(bytes.size)
            for (i in offset until end) {
                crc = crc xor (bytes[i].toInt() and 0xFF)
                for (j in 0 until 8) {
                    if ((crc and 0x0001) != 0) {
                        crc = (crc shr 1) xor 0xA001
                    } else {
                        crc = crc shr 1
                    }
                }
            }
            return crc and 0xFFFF
        }
    }
}
