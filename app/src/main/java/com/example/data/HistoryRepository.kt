package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val commandText: String,
    val isHex: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val isSuccess: Boolean = true
)

class HistoryRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("serial_history_prefs", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val jsonStr = prefs.getString(KEY_HISTORY, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<HistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            commandText = obj.getString("commandText"),
                            isHex = obj.optBoolean("isHex", false),
                            timestamp = obj.optString("timestamp", ""),
                            isSuccess = obj.optBoolean("isSuccess", true)
                        )
                    )
                }
                _history.value = list
                return
            } catch (_: Exception) {}
        }
        // Defaults
        _history.value = listOf(
            HistoryItem(commandText = "GET,ALL", isHex = false),
            HistoryItem(commandText = "SET,gDebug,0", isHex = false),
            HistoryItem(commandText = "02 01 03 02 03", isHex = true)
        )
    }

    fun recordSend(text: String, isHex: Boolean, isSuccess: Boolean = true) {
        if (text.isBlank()) return
        val current = _history.value.toMutableList()
        // Remove duplicate if same text exists recently
        current.removeAll { it.commandText == text && it.isHex == isHex }
        // Prepend to top
        current.add(0, HistoryItem(commandText = text, isHex = isHex, isSuccess = isSuccess))
        // Cap to 100 entries
        if (current.size > 100) {
            current.removeAt(current.lastIndex)
        }
        _history.value = current
        saveHistory(current)
    }

    fun deleteItem(id: String) {
        val current = _history.value.filter { it.id != id }
        _history.value = current
        saveHistory(current)
    }

    fun clearAll() {
        _history.value = emptyList()
        saveHistory(emptyList())
    }

    private fun saveHistory(list: List<HistoryItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("commandText", item.commandText)
                put("isHex", item.isHex)
                put("timestamp", item.timestamp)
                put("isSuccess", item.isSuccess)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val KEY_HISTORY = "history_entries"
    }
}
