package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.oscilloscope.*
import com.example.protocol.*
import com.example.serial.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

enum class ScreenLayoutMode {
    SPLIT,           // 同屏模式: 上半区曲线，下半区终端/组件
    FULL_SCOPE,      // 全屏曲线
    FULL_WIDGETS,    // 全屏动态组件
    FULL_TERMINAL,   // 全屏终端
    LANDSCAPE_SCOPE  // 横屏全屏沉浸示波器
}

enum class LowerPaneTab {
    TERMINAL,
    WIDGETS
}

data class TerminalLine(
    val id: Long,
    val timestamp: String,
    val isTx: Boolean,
    val text: String,
    val hexText: String
)

data class DynamicWidgetData(
    val name: String,
    val rawValue: Double,
    val scaledValue: Double,
    val unit: String,
    val formula: String,
    val minVal: Double,
    val maxVal: Double,
    val isTxControl: Boolean = false,
    var txTargetValue: Double = 0.0
)

class SerialViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val historyRepo = HistoryRepository(context)
    val presetRepo = PresetRepository(context)

    private val usbEngine = UsbOtgSerialEngine(context, viewModelScope)
    private val virtualEngine = VirtualSignalEngine(viewModelScope)
    private var activeEngine: ISerialEngine = virtualEngine

    // UI & Layout States
    private val _layoutMode = MutableStateFlow(ScreenLayoutMode.SPLIT)
    val layoutMode = _layoutMode.asStateFlow()

    private val _lowerTab = MutableStateFlow(LowerPaneTab.TERMINAL)
    val lowerTab = _lowerTab.asStateFlow()

    // Serial & Connection States
    val serialStatus = activeEngine.status
    val trafficStats = activeEngine.trafficStats
    private val _serialConfig = MutableStateFlow(SerialConfig(portName = "Virtual Demo (内置仿真流)", isVirtual = true))
    val serialConfig = _serialConfig.asStateFlow()

    private val _availablePorts = MutableStateFlow<List<String>>(emptyList())
    val availablePorts = _availablePorts.asStateFlow()

    // Protocol Configuration
    private val _protocolConfig = MutableStateFlow(ProtocolConfig())
    val protocolConfig = _protocolConfig.asStateFlow()
    private val parser = ProtocolParser(_protocolConfig.value)

    // Dynamic Widgets Map
    private val _dynamicWidgets = MutableStateFlow<List<DynamicWidgetData>>(emptyList())
    val dynamicWidgets = _dynamicWidgets.asStateFlow()

    // TX Parameter Target Values
    private val _txParams = MutableStateFlow<Map<String, Double>>(mapOf("target_speed" to 1500.0, "kp" to 5.0))
    val txParams = _txParams.asStateFlow()

    // 20 Channels for Oscilloscope
    private val _channels = MutableStateFlow<List<ScopeChannelState>>(emptyList())
    val channels = _channels.asStateFlow()

    // Scope View State
    private val _scopeState = MutableStateFlow(ScopeViewState())
    val scopeState = _scopeState.asStateFlow()

    // Terminal Buffer
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines = _terminalLines.asStateFlow()

    private val _isHexDisplay = MutableStateFlow(false)
    val isHexDisplay = _isHexDisplay.asStateFlow()

    private val _showTimestamps = MutableStateFlow(true)
    val showTimestamps = _showTimestamps.asStateFlow()

    private val _showLineNumbers = MutableStateFlow(true)
    val showLineNumbers = _showLineNumbers.asStateFlow()

    private val _isTerminalPaused = MutableStateFlow(false)
    val isTerminalPaused = _isTerminalPaused.asStateFlow()

    // Send Bar State
    private val _sendText = MutableStateFlow("")
    val sendText = _sendText.asStateFlow()

    private val _isHexSend = MutableStateFlow(false)
    val isHexSend = _isHexSend.asStateFlow()

    private var lineIdGen = 0L
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var autoSendJob: Job? = null

    init {
        initChannels()
        refreshAvailablePorts()
        observeActiveEngine()
        // Connect to virtual demo by default for immediate out-of-the-box demo
        viewModelScope.launch {
            activeEngine.connect(_serialConfig.value)
        }
    }

    private fun initChannels() {
        val list = mutableListOf<ScopeChannelState>()
        for (i in 0 until 20) {
            val colorPair = TRACE_COLORS[i % TRACE_COLORS.size]
            val defaultName = when (i) {
                0 -> "f27 (转速)"
                1 -> "43dt (角速度)"
                2 -> "uv5 (电压)"
                3 -> "key (状态)"
                else -> "CH${i + 1}"
            }
            list.add(
                ScopeChannelState(
                    id = i,
                    name = defaultName,
                    color = colorPair.first,
                    colorHex = colorPair.second,
                    isVisible = i < 4,
                    windowIndex = if (i == 1) 1 else 0,
                    unit = when (i) {
                        0 -> "rpm"
                        1 -> "°/s"
                        2 -> "V"
                        else -> ""
                    }
                )
            )
        }
        _channels.value = list
    }

    fun refreshAvailablePorts() {
        val usbPorts = usbEngine.listAvailablePorts()
        val virtPorts = virtualEngine.listAvailablePorts()
        _availablePorts.value = usbPorts + virtPorts
    }

    fun toggleConnection() {
        viewModelScope.launch {
            if (activeEngine.status.value is SerialStatus.Connected) {
                activeEngine.disconnect()
            } else {
                val cfg = _serialConfig.value
                val isVirt = cfg.portName.contains("Virtual", ignoreCase = true) || cfg.isVirtual
                val targetEngine = if (isVirt) virtualEngine else usbEngine
                if (targetEngine != activeEngine) {
                    activeEngine.disconnect()
                    activeEngine = targetEngine
                    observeActiveEngine()
                }
                activeEngine.connect(cfg)
            }
        }
    }

    fun setPort(port: String) {
        val isVirt = port.contains("Virtual", ignoreCase = true)
        _serialConfig.value = _serialConfig.value.copy(portName = port, isVirtual = isVirt)
    }

    fun setBaudRate(baud: Int) {
        _serialConfig.value = _serialConfig.value.copy(baudRate = baud)
        if (activeEngine.status.value is SerialStatus.Connected) {
            viewModelScope.launch {
                activeEngine.disconnect()
                activeEngine.connect(_serialConfig.value)
            }
        }
    }

    fun setAdvancedSerialParams(dataBits: Int, stopBits: Int, parity: Int) {
        _serialConfig.value = _serialConfig.value.copy(dataBits = dataBits, stopBits = stopBits, parity = parity)
    }

    private var engineJob: Job? = null

    private fun observeActiveEngine() {
        engineJob?.cancel()
        engineJob = viewModelScope.launch(Dispatchers.Default) {
            activeEngine.incomingData.collect { bytes ->
                handleIncomingBytes(bytes)
            }
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val isHexMode = _protocolConfig.value.isHexMode
        val rawText = String(bytes, Charsets.UTF_8)
        val hexStr = ProtocolParser.byteArrayToHexString(bytes)

        // 1. Terminal Log Buffer
        if (!_isTerminalPaused.value) {
            val line = TerminalLine(
                id = ++lineIdGen,
                timestamp = timeFormat.format(Date()),
                isTx = false,
                text = rawText.trimEnd('\r', '\n'),
                hexText = hexStr
            )
            val currentList = _terminalLines.value.toMutableList()
            currentList.add(line)
            if (currentList.size > 2000) {
                currentList.removeAt(0)
            }
            _terminalLines.value = currentList
        }

        // 2. Protocol Parsing & Formula Scaling
        val parsedRows = if (isHexMode) {
            parser.parseIncomingBytes(bytes)
        } else {
            parser.parseIncomingText(rawText)
        }

        if (parsedRows.isNotEmpty()) {
            val chList = _channels.value
            for (row in parsedRows) {
                for (item in row) {
                    val chIdx = item.channelIndex
                    if (chIdx in chList.indices) {
                        val ch = chList[chIdx]
                        ch.buffer.append(item.scaledValue.toFloat())
                    }
                }
            }

            // Update latest readings on UI
            val latestRow = parsedRows.last()
            val updatedChs = chList.map { ch ->
                val matched = latestRow.find { it.channelIndex == ch.id }
                if (matched != null) {
                    ch.copy(
                        latestValue = matched.scaledValue.toFloat(),
                        minValue = ch.buffer.minVal,
                        maxValue = ch.buffer.maxVal,
                        unit = if (matched.unit.isNotEmpty()) matched.unit else ch.unit
                    )
                } else ch
            }
            _channels.value = updatedChs

            // Update Dynamic Widgets
            updateDynamicWidgets(latestRow)
        }
    }

    private fun updateDynamicWidgets(latestRow: List<ParsedChannelValue>) {
        val fields = _protocolConfig.value.textFields
        val widgets = mutableListOf<DynamicWidgetData>()

        for (item in latestRow) {
            val field = fields.find { it.name == item.name } ?: fields.getOrNull(item.channelIndex)
            val minV = field?.minVal ?: 0.0
            val maxV = field?.maxVal ?: 100.0
            val formula = field?.formula ?: "[x]"

            widgets.add(
                DynamicWidgetData(
                    name = item.name,
                    rawValue = item.rawValue,
                    scaledValue = item.scaledValue,
                    unit = item.unit.ifEmpty { field?.unit ?: "" },
                    formula = formula,
                    minVal = minV,
                    maxVal = maxV
                )
            )
        }
        _dynamicWidgets.value = widgets
    }

    fun updateProtocolConfig(newConfig: ProtocolConfig) {
        _protocolConfig.value = newConfig
        parser.updateConfig(newConfig)
        // Sync channel names with protocol fields
        val chList = _channels.value.toMutableList()
        newConfig.textFields.forEachIndexed { i, f ->
            if (i < chList.size) {
                chList[i] = chList[i].copy(name = f.name, unit = f.unit)
            }
        }
        _channels.value = chList
    }

    // Oscilloscope View Controls
    fun setZoomMode(mode: ZoomMode) {
        _scopeState.value = _scopeState.value.copy(zoomMode = mode)
    }

    fun zoomIn() {
        val current = _scopeState.value
        val newPts = max(50, (current.visiblePoints * 0.75).toInt())
        _scopeState.value = current.copy(visiblePoints = newPts)
    }

    fun zoomOut() {
        val current = _scopeState.value
        val newPts = min(20000, (current.visiblePoints * 1.35).toInt())
        _scopeState.value = current.copy(visiblePoints = newPts)
    }

    fun panPoints(deltaPoints: Long) {
        val current = _scopeState.value
        val newOffset = max(0L, current.panOffsetPoints + deltaPoints)
        _scopeState.value = current.copy(panOffsetPoints = newOffset)
    }

    fun snapToLatest() {
        _scopeState.value = _scopeState.value.copy(panOffsetPoints = 0L)
    }

    fun toggleScopePause() {
        _scopeState.value = _scopeState.value.copy(isPaused = !_scopeState.value.isPaused)
    }

    fun toggleAutoY() {
        _scopeState.value = _scopeState.value.copy(autoY = !_scopeState.value.autoY)
    }

    fun setSplitMode(mode: WindowSplitMode) {
        _scopeState.value = _scopeState.value.copy(splitMode = mode)
    }

    fun toggleChannelVisibility(channelId: Int) {
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(isVisible = !it.isVisible) else it
        }
    }

    fun updateChannelConfig(channelId: Int, name: String, color: androidx.compose.ui.graphics.Color, colorHex: String, windowIdx: Int) {
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(name = name, color = color, colorHex = colorHex, windowIndex = windowIdx) else it
        }
    }

    // Dynamic Parameter Interaction & Auto-Send
    fun onTxParamChanged(paramKey: String, newValue: Double) {
        val updated = _txParams.value.toMutableMap()
        updated[paramKey] = newValue
        _txParams.value = updated

        if (_protocolConfig.value.autoSendOnParamChange) {
            autoSendJob?.cancel()
            autoSendJob = viewModelScope.launch {
                delay(150) // 150ms debounce
                sendFormattedTxTemplate()
            }
        }
    }

    fun sendFormattedTxTemplate() {
        val text = parser.formatTxTemplate(_txParams.value)
        sendRaw(text, isHex = false)
    }

    // Send Actions
    fun setSendText(text: String) {
        _sendText.value = text
    }

    fun toggleHexSend() {
        _isHexSend.value = !_isHexSend.value
    }

    fun sendCurrent() {
        val text = _sendText.value.trim()
        if (text.isEmpty()) return
        sendRaw(text, _isHexSend.value)
    }

    fun sendRaw(cmdText: String, isHex: Boolean, appendCrLf: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = if (isHex) {
                try {
                    ProtocolParser.hexStringToByteArray(cmdText)
                } catch (_: Exception) {
                    return@launch
                }
            } else {
                val fullText = if (appendCrLf && !cmdText.endsWith("\n") && !cmdText.endsWith("\r")) {
                    "$cmdText\r\n"
                } else cmdText
                fullText.toByteArray(Charsets.UTF_8)
            }

            val success = activeEngine.send(bytes)

            // Record to Terminal
            withContext(Dispatchers.Main) {
                val line = TerminalLine(
                    id = ++lineIdGen,
                    timestamp = timeFormat.format(Date()),
                    isTx = true,
                    text = cmdText,
                    hexText = ProtocolParser.byteArrayToHexString(bytes)
                )
                val current = _terminalLines.value.toMutableList()
                current.add(line)
                _terminalLines.value = current

                // Automatically record to persistent history
                historyRepo.recordSend(cmdText, isHex, success)
            }
        }
    }

    // Terminal Controls
    fun toggleHexDisplay() {
        _isHexDisplay.value = !_isHexDisplay.value
    }

    fun toggleTimestamps() {
        _showTimestamps.value = !_showTimestamps.value
    }

    fun toggleLineNumbers() {
        _showLineNumbers.value = !_showLineNumbers.value
    }

    fun toggleTerminalPause() {
        _isTerminalPaused.value = !_isTerminalPaused.value
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun clearScopeBuffers() {
        _channels.value.forEach { it.buffer.clear() }
        _channels.value = _channels.value.map { it.copy(latestValue = 0f, minValue = 0f, maxValue = 0f) }
    }

    // Layout Switching
    fun setLayoutMode(mode: ScreenLayoutMode) {
        _layoutMode.value = mode
    }

    fun setLowerTab(tab: LowerPaneTab) {
        _lowerTab.value = tab
    }
}
