package com.example.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UsbOtgSerialEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ISerialEngine {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _status = MutableStateFlow<SerialStatus>(SerialStatus.Disconnected)
    override val status = _status.asStateFlow()

    private val _incomingData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingData = _incomingData.asSharedFlow()

    private val _trafficStats = MutableStateFlow(SerialTrafficStats())
    override val trafficStats = _trafficStats.asStateFlow()

    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var readJob: Job? = null

    override fun listAvailablePorts(): List<String> {
        val manager = usbManager ?: return emptyList()
        val list = mutableListOf<String>()
        for ((_, device) in manager.deviceList) {
            val chipName = identifyChipName(device.vendorId, device.productId)
            list.add("${device.deviceName} ($chipName, VID:0x%04X)".format(device.vendorId))
        }
        return list
    }

    override suspend fun connect(config: SerialConfig): Boolean = withContext(Dispatchers.IO) {
        val manager = usbManager ?: run {
            _status.value = SerialStatus.Error("USB 主机服务不可用")
            return@withContext false
        }

        val devices = manager.deviceList.values
        if (devices.isEmpty()) {
            _status.value = SerialStatus.Error("未检测到 USB-TTL 硬件设备，可切换为【内置仿真流】")
            return@withContext false
        }

        val device = devices.first()
        _status.value = SerialStatus.Connecting

        if (!manager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            manager.requestPermission(device, permissionIntent)
            _status.value = SerialStatus.Error("正在请求 USB 权限，请在手机弹窗中点击允许")
            return@withContext false
        }

        val connection = manager.openDevice(device) ?: run {
            _status.value = SerialStatus.Error("打开 USB 设备失败，请重新插拔")
            return@withContext false
        }

        // Find communication interface & endpoints
        var foundInterface: UsbInterface? = null
        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null

        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            var currentIn: UsbEndpoint? = null
            var currentOut: UsbEndpoint? = null

            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) currentIn = ep
                    else currentOut = ep
                }
            }
            if (currentIn != null && currentOut != null) {
                foundInterface = intf
                epIn = currentIn
                epOut = currentOut
                break
            }
        }

        if (foundInterface == null || epIn == null || epOut == null) {
            connection.close()
            _status.value = SerialStatus.Error("未找到串口传输端点")
            return@withContext false
        }

        if (!connection.claimInterface(foundInterface, true)) {
            connection.close()
            _status.value = SerialStatus.Error("未能占用 USB 接口")
            return@withContext false
        }

        // Configure Baud rate using standard CDC Line Coding (0x20, SET_LINE_CODING)
        setLineCoding(connection, config.baudRate, config.dataBits, config.stopBits, config.parity)

        usbConnection = connection
        usbInterface = foundInterface
        endpointIn = epIn
        endpointOut = epOut

        _status.value = SerialStatus.Connected(device.deviceName, config.baudRate)

        // Start background reading coroutine
        startReading(connection, epIn)
        return@withContext true
    }

    private fun setLineCoding(
        connection: UsbDeviceConnection,
        baudRate: Int,
        dataBits: Int,
        stopBits: Int,
        parity: Int
    ) {
        val lineCoding = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(baudRate)
            put((if (stopBits == 2) 2 else 0).toByte()) // 0: 1 stop bit, 2: 2 stop bits
            put(parity.toByte())                         // 0: None, 1: Odd, 2: Even
            put(dataBits.toByte())                       // 8, 7, etc.
        }.array()

        // Standard CDC-ACM request
        connection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 500)
    }

    private fun startReading(connection: UsbDeviceConnection, epIn: UsbEndpoint) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            var rxTotal = _trafficStats.value.rxBytes
            var txTotal = _trafficStats.value.txBytes
            var frames = _trafficStats.value.frameCount

            while (isActive) {
                val bytesRead = connection.bulkTransfer(epIn, buffer, buffer.size, 100)
                if (bytesRead > 0) {
                    val slice = buffer.copyOf(bytesRead)
                    rxTotal += bytesRead
                    frames++
                    _trafficStats.value = _trafficStats.value.copy(
                        rxBytes = rxTotal,
                        txBytes = txTotal,
                        frameCount = frames
                    )
                    _incomingData.emit(slice)
                }
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        readJob?.cancel()
        readJob = null
        usbInterface?.let { usbConnection?.releaseInterface(it) }
        usbConnection?.close()
        usbConnection = null
        usbInterface = null
        endpointIn = null
        endpointOut = null
        _status.value = SerialStatus.Disconnected
    }

    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val conn = usbConnection ?: return@withContext false
        val ep = endpointOut ?: return@withContext false

        val bytesSent = conn.bulkTransfer(ep, data, data.size, 1000)
        if (bytesSent >= 0) {
            val current = _trafficStats.value
            _trafficStats.value = current.copy(
                txBytes = current.txBytes + bytesSent
            )
            return@withContext true
        }
        return@withContext false
    }

    private fun identifyChipName(vid: Int, pid: Int): String = when (vid) {
        0x1A86 -> "CH340/CH341 (沁恒)"
        0x10C4 -> "CP210x (Silicon Labs)"
        0x0403 -> "FTDI (FT232)"
        0x067B -> "PL2303 (Prolific)"
        0x2341, 0x2A03 -> "Arduino / CDC-ACM"
        0x2E8A -> "Raspberry Pi Pico"
        0x303A -> "ESP32 USB Serial"
        else -> "USB Serial Device"
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.serial.USB_PERMISSION"
    }
}
