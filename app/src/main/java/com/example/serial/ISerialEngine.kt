package com.example.serial

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class SerialConfig(
    val portName: String = "Virtual Demo",
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0, // 0: None, 1: Odd, 2: Even
    val isVirtual: Boolean = true,
    val autoReconnect: Boolean = true
)

sealed class SerialStatus {
    object Disconnected : SerialStatus()
    object Connecting : SerialStatus()
    data class Connected(val portName: String, val baudRate: Int) : SerialStatus()
    data class Error(val message: String) : SerialStatus()
}

data class SerialTrafficStats(
    val txBytes: Long = 0L,
    val rxBytes: Long = 0L,
    val frameCount: Long = 0L,
    val txRateBps: Long = 0L,
    val rxRateBps: Long = 0L
)

interface ISerialEngine {
    val status: StateFlow<SerialStatus>
    val incomingData: SharedFlow<ByteArray>
    val trafficStats: StateFlow<SerialTrafficStats>

    suspend fun connect(config: SerialConfig): Boolean
    suspend fun disconnect()
    suspend fun send(data: ByteArray): Boolean
    fun listAvailablePorts(): List<String>
}
