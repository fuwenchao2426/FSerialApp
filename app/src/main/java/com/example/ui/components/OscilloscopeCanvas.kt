package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oscilloscope.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun OscilloscopeCanvasView(
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
    onToggleFullscreen: () -> Unit,
    onEnterLandscape: () -> Unit,
    onOpenChannelSettings: (ScopeChannelState) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val pointsBuffer = remember { FloatArray(4096) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .testTag("oscilloscope_card"),
        color = CardSurface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Floating / Toolbar strip
            ScopeTopToolbar(
                scopeState = scopeState,
                onZoomModeChanged = onZoomModeChanged,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onSnapToLatest = onSnapToLatest,
                onTogglePause = onTogglePause,
                onToggleAutoY = onToggleAutoY,
                onToggleSplit = onToggleSplit,
                onClearBuffers = onClearBuffers,
                onToggleFullscreen = onToggleFullscreen,
                onEnterLandscape = onEnterLandscape
            )

            // Canvas drawing area with gestures
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(OscilloscopeDarkBg)
                    .pointerInput(scopeState.zoomMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (abs(zoom - 1f) > 0.02f) {
                                if (zoom > 1f) onZoomIn() else onZoomOut()
                            }
                            if (abs(pan.x) > 2f) {
                                val delta = (-pan.x * (scopeState.visiblePoints / 300f)).toLong()
                                onPanPoints(delta)
                            }
                        }
                    }
                    .testTag("scope_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@Canvas

                    val isSplit = scopeState.splitMode == WindowSplitMode.SPLIT_2
                    if (isSplit) {
                        val subH = h / 2f
                        drawScopeWindow(
                            windowIndex = 0,
                            channels = channels.filter { it.windowIndex == 0 && it.isVisible },
                            scopeState = scopeState,
                            topY = 0f,
                            height = subH,
                            width = w,
                            pointsBuffer = pointsBuffer,
                            textMeasurer = textMeasurer,
                            title = "视窗 1 (主控/速度)"
                        )
                        drawLine(
                            color = PrimaryBlue.copy(alpha = 0.5f),
                            start = Offset(0f, subH),
                            end = Offset(w, subH),
                            strokeWidth = 2f
                        )
                        drawScopeWindow(
                            windowIndex = 1,
                            channels = channels.filter { it.windowIndex == 1 && it.isVisible },
                            scopeState = scopeState,
                            topY = subH,
                            height = subH,
                            width = w,
                            pointsBuffer = pointsBuffer,
                            textMeasurer = textMeasurer,
                            title = "视窗 2 (电压/辅助)"
                        )
                    } else {
                        drawScopeWindow(
                            windowIndex = 0,
                            channels = channels.filter { it.isVisible },
                            scopeState = scopeState,
                            topY = 0f,
                            height = h,
                            width = w,
                            pointsBuffer = pointsBuffer,
                            textMeasurer = textMeasurer,
                            title = null
                        )
                    }
                }

                if (scopeState.panOffsetPoints > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clickable { onSnapToLatest() },
                        shape = RoundedCornerShape(16.dp),
                        color = StatusAmber.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("回溯历史: -${scopeState.panOffsetPoints}点", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("回到实时 ⏩", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Channel Legend horizontal chips
            ChannelLegendBar(
                channels = channels,
                onToggleChannelVisibility = onToggleChannelVisibility,
                onOpenChannelSettings = onOpenChannelSettings
            )
        }
    }
}

private fun DrawScope.drawScopeWindow(
    windowIndex: Int,
    channels: List<ScopeChannelState>,
    scopeState: ScopeViewState,
    topY: Float,
    height: Float,
    width: Float,
    pointsBuffer: FloatArray,
    textMeasurer: TextMeasurer,
    title: String?
) {
    val axisLeft = 55.dp.toPx()
    val axisRight = width - 10.dp.toPx()
    val axisTop = topY + 16.dp.toPx()
    val axisBottom = topY + height - 22.dp.toPx()
    val plotW = axisRight - axisLeft
    val plotH = axisBottom - axisTop
    if (plotW <= 0f || plotH <= 0f) return

    // 1. Calculate dynamic Y range based on autoY or manual limits
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (ch in channels) {
        if (ch.buffer.availableCount > 0) {
            if (ch.buffer.minVal < minY) minY = ch.buffer.minVal
            if (ch.buffer.maxVal > maxY) maxY = ch.buffer.maxVal
        }
    }
    if (minY == Float.MAX_VALUE || maxY == -Float.MAX_VALUE || abs(maxY - minY) < 1e-4f) {
        minY = scopeState.manualYMin
        maxY = scopeState.manualYMax
    } else if (scopeState.autoY) {
        val span = maxY - minY
        minY -= span * 0.1f
        maxY += span * 0.1f
    }
    val ySpan = max(1e-4f, maxY - minY)

    // 2. Draw Dynamic Grid & Axes with Nice Numbers
    val yDivisions = 5
    val yStep = ySpan / yDivisions
    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

    for (i in 0..yDivisions) {
        val gridY = axisBottom - (i.toFloat() / yDivisions) * plotH
        val yVal = minY + i * yStep
        drawLine(
            color = if (i == 0 || i == yDivisions) OscilloscopeGrid else OscilloscopeSubGrid,
            start = Offset(axisLeft, gridY),
            end = Offset(axisRight, gridY),
            strokeWidth = 1f,
            pathEffect = if (i == 0 || i == yDivisions) null else dashedEffect
        )
        val labelStr = formatNiceNumber(yVal)
        drawText(
            textMeasurer = textMeasurer,
            text = labelStr,
            topLeft = Offset(4.dp.toPx(), gridY - 8.dp.toPx()),
            style = TextStyle(color = OscilloscopeText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
    }

    // 3. Draw Dynamic X-axis points/indices
    val maxHead = channels.maxOfOrNull { it.buffer.totalCount } ?: 0L
    val endIdx = max(0L, maxHead - scopeState.panOffsetPoints)
    val startIdx = max(0L, endIdx - scopeState.visiblePoints)
    val xDivisions = 4

    for (j in 0..xDivisions) {
        val gridX = axisLeft + (j.toFloat() / xDivisions) * plotW
        val ptIdx = startIdx + (j.toFloat() / xDivisions * (endIdx - startIdx)).toLong()
        drawLine(
            color = OscilloscopeSubGrid,
            start = Offset(gridX, axisTop),
            end = Offset(gridX, axisBottom),
            strokeWidth = 1f,
            pathEffect = dashedEffect
        )
        val xLabel = "$ptIdx"
        drawText(
            textMeasurer = textMeasurer,
            text = xLabel,
            topLeft = Offset(gridX - 12.dp.toPx(), axisBottom + 2.dp.toPx()),
            style = TextStyle(color = OscilloscopeText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
    }

    // Window Title Tag
    if (title != null) {
        drawText(
            textMeasurer = textMeasurer,
            text = title,
            topLeft = Offset(axisLeft + 6.dp.toPx(), axisTop + 4.dp.toPx()),
            style = TextStyle(color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
    }

    // 4. Draw Channel Waves using downsampled points
    val targetBuckets = (plotW / 1.5f).toInt().coerceIn(100, 1000)

    for (ch in channels) {
        val count = ch.buffer.downsample(
            endGlobalIndex = endIdx,
            pointCount = scopeState.visiblePoints,
            targetBuckets = targetBuckets,
            outPoints = pointsBuffer
        )
        if (count < 2) continue

        val path = Path()
        var started = false

        for (p in 0 until count) {
            val xNorm = pointsBuffer[p * 2]
            val yVal = pointsBuffer[p * 2 + 1]

            val px = axisLeft + xNorm * plotW
            val py = axisBottom - ((yVal - minY) / ySpan) * plotH

            if (!started) {
                path.moveTo(px, py.coerceIn(axisTop - 20f, axisBottom + 20f))
                started = true
            } else {
                path.lineTo(px, py.coerceIn(axisTop - 20f, axisBottom + 20f))
            }
        }

        drawPath(
            path = path,
            color = ch.color,
            style = Stroke(width = 2.2f)
        )
    }
}

private fun formatNiceNumber(v: Float): String {
    val absV = abs(v)
    return when {
        absV >= 10000f -> "%.0f".format(v)
        absV >= 100f -> "%.0f".format(v)
        absV >= 10f -> "%.1f".format(v)
        absV >= 1f -> "%.2f".format(v)
        absV > 0f -> "%.3f".format(v)
        else -> "0"
    }
}

@Composable
private fun ScopeTopToolbar(
    scopeState: ScopeViewState,
    onZoomModeChanged: (ZoomMode) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSnapToLatest: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleAutoY: () -> Unit,
    onToggleSplit: () -> Unit,
    onClearBuffers: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onEnterLandscape: () -> Unit
) {
    Surface(
        color = Color(0xFFF1F5F9),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Zoom mode toggle chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ZoomMode.values().forEach { mode ->
                    val isSelected = scopeState.zoomMode == mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onZoomModeChanged(mode) },
                        color = if (isSelected) PrimaryBlue else Color.Transparent,
                        contentColor = if (isSelected) Color.White else TextSecondary
                    ) {
                        Text(
                            text = mode.label.substring(0, 2),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Zoom +/-
                IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ZoomIn, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ZoomOut, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Auto Y Toggle
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleAutoY() },
                    color = if (scopeState.autoY) StatusGreenContainer else Color.Transparent,
                    border = if (scopeState.autoY) BorderStroke(1.dp, StatusGreen) else null
                ) {
                    Text(
                        text = "Auto-Y",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scopeState.autoY) StatusGreen else TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Split 2-window toggle
                IconButton(onClick = onToggleSplit, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = if (scopeState.splitMode == WindowSplitMode.SPLIT_2) Icons.Default.VerticalSplit else Icons.Default.Square,
                        contentDescription = "分屏切换",
                        tint = if (scopeState.splitMode == WindowSplitMode.SPLIT_2) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Pause / Play
                IconButton(onClick = onTogglePause, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = if (scopeState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "暂停/播放",
                        tint = if (scopeState.isPaused) StatusAmber else StatusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear Buffer
                IconButton(onClick = onClearBuffers, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.DeleteSweep, "清除波形", tint = TextMuted, modifier = Modifier.size(16.dp))
                }

                // Rotate to Landscape Fullscreen
                IconButton(onClick = onEnterLandscape, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ScreenRotation, "横屏全屏", tint = TilePurple, modifier = Modifier.size(16.dp))
                }

                // Fullscreen toggle
                IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Fullscreen, "全屏", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChannelLegendBar(
    channels: List<ScopeChannelState>,
    onToggleChannelVisibility: (Int) -> Unit,
    onOpenChannelSettings: (ScopeChannelState) -> Unit
) {
    Surface(
        color = CardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            channels.forEach { ch ->
                val isVisible = ch.isVisible
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isVisible) ch.color else BorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleChannelVisibility(ch.id) },
                    color = if (isVisible) ch.color.copy(alpha = 0.12f) else Color(0xFFF8FAFC)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isVisible) ch.color else Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ch.name,
                            fontSize = 11.sp,
                            fontWeight = if (isVisible) FontWeight.Bold else FontWeight.Normal,
                            color = if (isVisible) TextPrimary else TextMuted
                        )
                        if (isVisible) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1f%s".format(ch.latestValue, if (ch.unit.isNotEmpty()) " ${ch.unit}" else ""),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ch.color
                            )
                        }
                    }
                }
            }
        }
    }
}
