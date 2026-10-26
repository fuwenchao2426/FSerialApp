package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

enum class SuffixType(val label: String, val chars: String) {
    NONE("无", ""),
    CR_LF("\\r\\n (回车换行)", "\r\n"),
    CR("\\r", "\r"),
    LF("\\n", "\n")
}

data class PresetCommand(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val commandText: String,
    val isHex: Boolean = false,
    val suffix: SuffixType = SuffixType.CR_LF,
    val colorHex: String = "#2563EB"
)

class PresetRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("serial_presets_prefs", Context.MODE_PRIVATE)
    private val _presets = MutableStateFlow<List<PresetCommand>>(emptyList())
    val presets = _presets.asStateFlow()

    init {
        loadPresets()
    }

    private fun loadPresets() {
        val jsonStr = prefs.getString(KEY_PRESETS, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<PresetCommand>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PresetCommand(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            label = obj.getString("label"),
                            commandText = obj.getString("commandText"),
                            isHex = obj.optBoolean("isHex", false),
                            suffix = SuffixType.valueOf(obj.optString("suffix", SuffixType.CR_LF.name)),
                            colorHex = obj.optString("colorHex", "#2563EB")
                        )
                    )
                }
                _presets.value = list
                return
            } catch (_: Exception) {}
        }
        // Defaults matching typical embedded debugging macros
        _presets.value = listOf(
            PresetCommand(label = "GET_ALL", commandText = "GET,ALL", colorHex = "#0284C7"),
            PresetCommand(label = "AT", commandText = "AT", colorHex = "#10B981"),
            PresetCommand(label = "AT+RST", commandText = "AT+RST", colorHex = "#EF4444"),
            PresetCommand(label = "START", commandText = "START", colorHex = "#8B5CF6"),
            PresetCommand(label = "STOP", commandText = "STOP", colorHex = "#F59E0B"),
            PresetCommand(label = "HEX_PING", commandText = "02 01 03 02 03", isHex = true, colorHex = "#0D9488")
        )
    }

    fun addPreset(label: String, commandText: String, isHex: Boolean = false, suffix: SuffixType = SuffixType.CR_LF, colorHex: String = "#2563EB") {
        val current = _presets.value.toMutableList()
        current.add(PresetCommand(label = label, commandText = commandText, isHex = isHex, suffix = suffix, colorHex = colorHex))
        _presets.value = current
        savePresets(current)
    }

    fun deletePreset(id: String) {
        val current = _presets.value.filter { it.id != id }
        _presets.value = current
        savePresets(current)
    }

    private fun savePresets(list: List<PresetCommand>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("commandText", item.commandText)
                put("isHex", item.isHex)
                put("suffix", item.suffix.name)
                put("colorHex", item.colorHex)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    companion object {
        private const val KEY_PRESETS = "preset_commands"
    }
}
