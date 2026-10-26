package com.example.oscilloscope

import androidx.compose.ui.graphics.Color

enum class ZoomMode(val label: String) {
    X_AXIS("X轴缩放"),
    Y_AXIS("Y轴缩放"),
    UNIFORM_XY("XY自由缩放")
}

enum class WindowSplitMode(val label: String) {
    SINGLE("单视窗合并"),
    SPLIT_2("双视窗分屏")
}

data class ScopeChannelState(
    val id: Int,
    val name: String,
    val color: Color,
    val colorHex: String,
    val isVisible: Boolean = true,
    val windowIndex: Int = 0,
    val unit: String = "",
    val latestValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 0f,
    val buffer: FloatRingBuffer = FloatRingBuffer(100_000)
)

data class ScopeViewState(
    val zoomMode: ZoomMode = ZoomMode.X_AXIS,
    val visiblePoints: Int = 500,
    val panOffsetPoints: Long = 0L,
    val isPaused: Boolean = false,
    val autoY: Boolean = true,
    val manualYMin: Float = -10f,
    val manualYMax: Float = 100f,
    val splitMode: WindowSplitMode = WindowSplitMode.SINGLE,
    val showGrid: Boolean = true
)

val TRACE_COLORS = listOf(
    Color(0xFF00E5FF) to "#00E5FF", // Neon Cyan
    Color(0xFF76FF03) to "#76FF03", // Lime Green
    Color(0xFFFFD600) to "#FFD600", // Amber Yellow
    Color(0xFFFF3D00) to "#FF3D00", // Safety Orange
    Color(0xFFE040FB) to "#E040FB", // Neon Magenta
    Color(0xFF00E676) to "#00E676", // Mint Emerald
    Color(0xFF2979FF) to "#2979FF", // Electric Blue
    Color(0xFFFF1744) to "#FF1744", // Crimson Red
    Color(0xFFAEEA00) to "#AEEA00", // Chartreuse
    Color(0xFFAA00FF) to "#AA00FF", // Violet
    Color(0xFFFF9100) to "#FF9100", // Bright Orange
    Color(0xFF1DE9B6) to "#1DE9B6", // Turquoise
    Color(0xFFFF4081) to "#FF4081", // Rose Pink
    Color(0xFF651FFF) to "#651FFF", // Deep Purple
    Color(0xFF00B0FF) to "#00B0FF", // Light Blue
    Color(0xFF795548) to "#795548", // Warm Bronze
    Color(0xFF9E9E9E) to "#9E9E9E", // Silver Grey
    Color(0xFF64DD17) to "#64DD17", // Light Lime
    Color(0xFFFFAB00) to "#FFAB00", // Gold
    Color(0xFF00BFA5) to "#00BFA5"  // Teal
)
