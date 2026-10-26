package com.example.serial

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class VirtualSignalEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ISerialEngine {

    private val _status = MutableStateFlow<SerialStatus>(SerialStatus.Disconnected)
    override val status = _status.asStateFlow()

    private val _incomingData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingData = _incomingData.asSharedFlow()

    private val _trafficStats = MutableStateFlow(SerialTrafficStats())
    override val trafficStats = _trafficStats.asStateFlow()

    private var generatorJob: Job? = null
    private var timeTick = 0L

    override suspend fun connect(config: SerialConfig): Boolean {
        _status.value = SerialStatus.Connecting
        delay(150)
        _status.value = SerialStatus.Connected(config.portName, config.baudRate)

        generatorJob?.cancel()
        generatorJob = scope.launch {
            var txTotal = _trafficStats.value.txBytes
            var rxTotal = _trafficStats.value.rxBytes
            var frames = _trafficStats.value.frameCount

            while (isActive) {
                timeTick++
                val t = timeTick.toDouble()

                // Generate 4 dynamic simulated channel raw values
                val val0 = (2500 + 1500 * sin(t * 0.08) + Random.nextDouble(-30.0, 30.0)).toInt()
                val val1 = (16384 * sin(t * 0.04) + Random.nextDouble(-100.0, 100.0)).toInt()
                val val2 = (2048 + 1200 * cos(t * 0.06)).toInt()
                val val3 = if ((timeTick / 25) % 2 == 0L) 1200 else 200

                // CSV packet: KS,val0,val1,val2,val3\r\n
                val packetStr = "KS,$val0,$val1,$val2,$val3\r\n"
                val bytes = packetStr.toByteArray(Charsets.UTF_8)

                rxTotal += bytes.size
                frames++
                _trafficStats.value = _trafficStats.value.copy(
                    rxBytes = rxTotal,
                    txBytes = txTotal,
                    frameCount = frames
                )

                _incomingData.emit(bytes)
                delay(20) // 50 Hz streaming update
            }
        }
        return true
    }

    override suspend fun disconnect() {
        generatorJob?.cancel()
        generatorJob = null
        _status.value = SerialStatus.Disconnected
    }

    override suspend fun send(data: ByteArray): Boolean {
        if (_status.value !is SerialStatus.Connected) return false
        val current = _trafficStats.value
        _trafficStats.value = current.copy(
            txBytes = current.txBytes + data.size
        )
        // Echo response or acknowledge
        delay(10)
        val ackMsg = "ACK: received ${data.size} bytes\r\n".toByteArray(Charsets.UTF_8)
        _incomingData.emit(ackMsg)
        return true
    }

    override fun listAvailablePorts(): List<String> {
        return listOf("Virtual Demo (内置仿真流)", "Virtual Sensor Stream (50Hz)", "Virtual Sine Wave (100Hz)")
    }
}
