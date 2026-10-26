package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HistoryItem
import com.example.data.PresetCommand
import com.example.oscilloscope.ScopeChannelState
import com.example.oscilloscope.WindowSplitMode
import com.example.protocol.ProtocolConfig
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SerialLabMainScreen(
    viewModel: SerialViewModel,
    modifier: Modifier = Modifier
) {
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val lowerTab by viewModel.lowerTab.collectAsStateWithLifecycle()

    val serialStatus by viewModel.serialStatus.collectAsStateWithLifecycle()
    val serialConfig by viewModel.serialConfig.collectAsStateWithLifecycle()
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val availablePorts by viewModel.availablePorts.collectAsStateWithLifecycle()

    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val scopeState by viewModel.scopeState.collectAsStateWithLifecycle()

    val dynamicWidgets by viewModel.dynamicWidgets.collectAsStateWithLifecycle()
    val txParams by viewModel.txParams.collectAsStateWithLifecycle()
    val protocolConfig by viewModel.protocolConfig.collectAsStateWithLifecycle()

    val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()
    val isHexDisplay by viewModel.isHexDisplay.collectAsStateWithLifecycle()
    val showTimestamps by viewModel.showTimestamps.collectAsStateWithLifecycle()
    val showLineNumbers by viewModel.showLineNumbers.collectAsStateWithLifecycle()
    val isTerminalPaused by viewModel.isTerminalPaused.collectAsStateWithLifecycle()

    val sendText by viewModel.sendText.collectAsStateWithLifecycle()
    val isHexSend by viewModel.isHexSend.collectAsStateWithLifecycle()
    val presets by viewModel.presetRepo.presets.collectAsStateWithLifecycle()
    val historyList by viewModel.historyRepo.history.collectAsStateWithLifecycle()

    // Modals state
    var showHistorySheet by remember { mutableStateOf(false) }
    var showFrameEditor by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var selectedChannelForConfig by remember { mutableStateOf<ScopeChannelState?>(null) }

    // Back button handling: exit fullscreen/landscape back to split mode
    BackHandler(enabled = layoutMode != ScreenLayoutMode.SPLIT) {
        viewModel.setLayoutMode(ScreenLayoutMode.SPLIT)
    }

    if (layoutMode == ScreenLayoutMode.LANDSCAPE_SCOPE) {
        LandscapeOscilloscopeScreen(
            channels = channels,
            scopeState = scopeState,
            onZoomModeChanged = { viewModel.setZoomMode(it) },
            onZoomIn = { viewModel.zoomIn() },
            onZoomOut = { viewModel.zoomOut() },
            onPanPoints = { viewModel.panPoints(it) },
            onSnapToLatest = { viewModel.snapToLatest() },
            onTogglePause = { viewModel.toggleScopePause() },
            onToggleAutoY = { viewModel.toggleAutoY() },
            onToggleSplit = {
                viewModel.setSplitMode(
                    if (scopeState.splitMode == WindowSplitMode.SINGLE) WindowSplitMode.SPLIT_2 else WindowSplitMode.SINGLE
                )
            },
            onToggleChannelVisibility = { viewModel.toggleChannelVisibility(it) },
            onClearBuffers = { viewModel.clearScopeBuffers() },
            onExitLandscape = { viewModel.setLayoutMode(ScreenLayoutMode.SPLIT) },
            onOpenChannelSettings = { selectedChannelForConfig = it },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopSerialBar(
                serialStatus = serialStatus,
                serialConfig = serialConfig,
                trafficStats = trafficStats,
                availablePorts = availablePorts,
                onToggleConnect = { viewModel.toggleConnection() },
                onPortSelected = { viewModel.setPort(it) },
                onBaudSelected = { viewModel.setBaudRate(it) },
                onOpenSettings = { showAdvancedSettings = true },
                onOpenFrameEditor = { showFrameEditor = true }
            )
        },
        bottomBar = {
            SendBar(
                sendText = sendText,
                isHexSend = isHexSend,
                presets = presets,
                onSendTextChanged = { viewModel.setSendText(it) },
                onToggleHexSend = { viewModel.toggleHexSend() },
                onSend = { viewModel.sendCurrent() },
                onSendPreset = { preset ->
                    viewModel.sendRaw(preset.commandText, preset.isHex)
                },
                onOpenHistory = { showHistorySheet = true },
                onAddPreset = { showAddPresetDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (layoutMode) {
                ScreenLayoutMode.SPLIT -> {
                    // Split Mode: Top is Oscilloscope (weight 1.15), Bottom is Tab switcher + (Terminal or Widgets)
                    OscilloscopeCanvasView(
                        channels = channels,
                        scopeState = scopeState,
                        onZoomModeChanged = { viewModel.setZoomMode(it) },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onPanPoints = { viewModel.panPoints(it) },
                        onSnapToLatest = { viewModel.snapToLatest() },
                        onTogglePause = { viewModel.toggleScopePause() },
                        onToggleAutoY = { viewModel.toggleAutoY() },
                        onToggleSplit = {
                            viewModel.setSplitMode(
                                if (scopeState.splitMode == WindowSplitMode.SINGLE) WindowSplitMode.SPLIT_2 else WindowSplitMode.SINGLE
                            )
                        },
                        onToggleChannelVisibility = { viewModel.toggleChannelVisibility(it) },
                        onClearBuffers = { viewModel.clearScopeBuffers() },
                        onToggleFullscreen = { viewModel.setLayoutMode(ScreenLayoutMode.FULL_SCOPE) },
                        onEnterLandscape = { viewModel.setLayoutMode(ScreenLayoutMode.LANDSCAPE_SCOPE) },
                        onOpenChannelSettings = { selectedChannelForConfig = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.15f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Lower Pane Segmented Selector (Terminal vs Dynamic Widgets)
                    Surface(
                        color = CardSurface,
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = lowerTab == LowerPaneTab.TERMINAL,
                                    onClick = { viewModel.setLowerTab(LowerPaneTab.TERMINAL) },
                                    label = { Text("📝 终端监视区", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = lowerTab == LowerPaneTab.WIDGETS,
                                    onClick = { viewModel.setLowerTab(LowerPaneTab.WIDGETS) },
                                    label = { Text("🎛️ 动态组件 (JCOM/调参)", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }

                            // Fullscreen icon for lower pane
                            IconButton(
                                onClick = {
                                    if (lowerTab == LowerPaneTab.TERMINAL)
                                        viewModel.setLayoutMode(ScreenLayoutMode.FULL_TERMINAL)
                                    else
                                        viewModel.setLayoutMode(ScreenLayoutMode.FULL_WIDGETS)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, "全屏", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Lower Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (lowerTab == LowerPaneTab.TERMINAL) {
                            TerminalView(
                                lines = terminalLines,
                                isHexDisplay = isHexDisplay,
                                showTimestamps = showTimestamps,
                                showLineNumbers = showLineNumbers,
                                isPaused = isTerminalPaused,
                                onToggleHex = { viewModel.toggleHexDisplay() },
                                onToggleTimestamps = { viewModel.toggleTimestamps() },
                                onToggleLineNumbers = { viewModel.toggleLineNumbers() },
                                onTogglePause = { viewModel.toggleTerminalPause() },
                                onClear = { viewModel.clearTerminal() },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            DynamicWidgetsView(
                                widgets = dynamicWidgets,
                                txParams = txParams,
                                autoSendEnabled = protocolConfig.autoSendOnParamChange,
                                onTxParamChanged = { k, v -> viewModel.onTxParamChanged(k, v) },
                                onSendSingleTxParam = { viewModel.sendFormattedTxTemplate() },
                                onOpenFrameEditor = { showFrameEditor = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                ScreenLayoutMode.FULL_SCOPE -> {
                    OscilloscopeCanvasView(
                        channels = channels,
                        scopeState = scopeState,
                        onZoomModeChanged = { viewModel.setZoomMode(it) },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onPanPoints = { viewModel.panPoints(it) },
                        onSnapToLatest = { viewModel.snapToLatest() },
                        onTogglePause = { viewModel.toggleScopePause() },
                        onToggleAutoY = { viewModel.toggleAutoY() },
                        onToggleSplit = {
                            viewModel.setSplitMode(
                                if (scopeState.splitMode == WindowSplitMode.SINGLE) WindowSplitMode.SPLIT_2 else WindowSplitMode.SINGLE
                            )
                        },
                        onToggleChannelVisibility = { viewModel.toggleChannelVisibility(it) },
                        onClearBuffers = { viewModel.clearScopeBuffers() },
                        onToggleFullscreen = { viewModel.setLayoutMode(ScreenLayoutMode.SPLIT) },
                        onEnterLandscape = { viewModel.setLayoutMode(ScreenLayoutMode.LANDSCAPE_SCOPE) },
                        onOpenChannelSettings = { selectedChannelForConfig = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScreenLayoutMode.FULL_TERMINAL -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("全屏终端视窗", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            IconButton(onClick = { viewModel.setLayoutMode(ScreenLayoutMode.SPLIT) }) {
                                Icon(Icons.Default.FullscreenExit, "还原同屏")
                            }
                        }
                        TerminalView(
                            lines = terminalLines,
                            isHexDisplay = isHexDisplay,
                            showTimestamps = showTimestamps,
                            showLineNumbers = showLineNumbers,
                            isPaused = isTerminalPaused,
                            onToggleHex = { viewModel.toggleHexDisplay() },
                            onToggleTimestamps = { viewModel.toggleTimestamps() },
                            onToggleLineNumbers = { viewModel.toggleLineNumbers() },
                            onTogglePause = { viewModel.toggleTerminalPause() },
                            onClear = { viewModel.clearTerminal() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScreenLayoutMode.FULL_WIDGETS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("全屏动态组件与参数调参面板", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            IconButton(onClick = { viewModel.setLayoutMode(ScreenLayoutMode.SPLIT) }) {
                                Icon(Icons.Default.FullscreenExit, "还原同屏")
                            }
                        }
                        DynamicWidgetsView(
                            widgets = dynamicWidgets,
                            txParams = txParams,
                            autoSendEnabled = protocolConfig.autoSendOnParamChange,
                            onTxParamChanged = { k, v -> viewModel.onTxParamChanged(k, v) },
                            onSendSingleTxParam = { viewModel.sendFormattedTxTemplate() },
                            onOpenFrameEditor = { showFrameEditor = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScreenLayoutMode.LANDSCAPE_SCOPE -> { /* Handled above */ }
            }
        }
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        HistoryBottomSheet(
            historyList = historyList,
            onResendItem = { item ->
                viewModel.sendRaw(item.commandText, item.isHex)
            },
            onFillInput = { item ->
                viewModel.setSendText(item.commandText)
                if (item.isHex != isHexSend) viewModel.toggleHexSend()
                showHistorySheet = false
            },
            onDeleteItem = { id -> viewModel.historyRepo.deleteItem(id) },
            onClearAll = { viewModel.historyRepo.clearAll() },
            onDismiss = { showHistorySheet = false }
        )
    }

    // Protocol Frame Editor Dialog
    if (showFrameEditor) {
        FrameEditorDialog(
            initialConfig = protocolConfig,
            onSave = { updated ->
                viewModel.updateProtocolConfig(updated)
                showFrameEditor = false
            },
            onDismiss = { showFrameEditor = false }
        )
    }

    // Advanced Serial Settings Dialog
    if (showAdvancedSettings) {
        AdvancedSettingsDialog(
            currentConfig = serialConfig,
            onSave = { dataBits, stopBits, parity ->
                viewModel.setAdvancedSerialParams(dataBits, stopBits, parity)
            },
            onDismiss = { showAdvancedSettings = false }
        )
    }

    // Channel Settings Sheet
    selectedChannelForConfig?.let { ch ->
        ChannelSettingsSheet(
            channel = ch,
            onSave = { name, color, colorHex, windowIdx ->
                viewModel.updateChannelConfig(ch.id, name, color, colorHex, windowIdx)
                selectedChannelForConfig = null
            },
            onDismiss = { selectedChannelForConfig = null }
        )
    }

    // Add Preset Dialog
    if (showAddPresetDialog) {
        AddPresetDialog(
            onAdd = { label, cmdText, isHex, suffix ->
                viewModel.presetRepo.addPreset(label, cmdText, isHex, suffix)
            },
            onDismiss = { showAddPresetDialog = false }
        )
    }
}
