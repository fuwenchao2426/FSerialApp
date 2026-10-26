package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oscilloscope.ScopeChannelState
import com.example.oscilloscope.ScopeViewState
import com.example.oscilloscope.ZoomMode
import com.example.ui.theme.*

@Composable
fun LandscapeOscilloscopeScreen(
    channels: List<ScopeChannelState>,
    scopeState: ScopeViewState,
    onZoomModeChanged: (ZoomMode) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPanPoints: (Long) -> Unit,
    onSnapToLatest: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleAutoY: () -> Unit,
    onToggleSplit: () -> Unit,
    onToggleChannelVisibility: (Int) -> Unit,
    onClearBuffers: () -> Unit,
    onExitLandscape: () -> Unit,
    onOpenChannelSettings: (ScopeChannelState) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OscilloscopeDarkBg)
            .testTag("landscape_scope_screen")
    ) {
        // Fullscreen Oscilloscope Canvas
        OscilloscopeCanvasView(
            channels = channels,
            scopeState = scopeState,
            onZoomModeChanged = onZoomModeChanged,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onPanPoints = onPanPoints,
            onSnapToLatest = onSnapToLatest,
            onTogglePause = onTogglePause,
            onToggleAutoY = onToggleAutoY,
            onToggleSplit = onToggleSplit,
            onToggleChannelVisibility = onToggleChannelVisibility,
            onClearBuffers = onClearBuffers,
            onToggleFullscreen = onExitLandscape,
            onEnterLandscape = onExitLandscape,
            onOpenChannelSettings = onOpenChannelSettings,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Exit
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExitLandscape, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "退出全屏",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
