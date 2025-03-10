package com.example.login

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class RootManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "RootPrefs"
        private const val KEY_ROOT_MODE = "root_mode"
        private const val DEV_DIAG = "/dev/diag"
        private const val FIFO_PATH = "diag_fifo"
        private const val EXEC_NAME = "diag_revealer"
        private const val LIB_NAME = "diag_revealer"

        init {
            Shell.enableVerboseLogging = true
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(60)
            )
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var diagReaderJob: Job? = null
    private var fifoReaderJob: Job? = null

    data class NetworkInfo(
        var cellId: Int = 0,
        var signalStrength: Int = 0,
        var networkType: String = "",
        var frequency: Int = 0,
        var bandwidth: Int = 0
    )

    sealed class NetworkMessage {
        data class GsmMessage(
            val channelType: Int,
            val messageLength: Int,
            val isUplink: Boolean
        ) : NetworkMessage()

        data class LteMessage(
            val pci: Int,
            val earfcn: Int,
            val signalStrength: Int
        ) : NetworkMessage()

        data class WcdmaMessage(
            val uarfcn: Int,
            val psc: Int,
            val signalStrength: Int
        ) : NetworkMessage()
    }

    fun isRootModeEnabled() = prefs.getBoolean(KEY_ROOT_MODE, false)

    fun setRootModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ROOT_MODE, enabled) }
        if (enabled) {
            requestRootAccess()
        } else {
            stopDiagReader()
        }
    }

    fun requestRootAccess() {
        if (!isDeviceArchitectureSupported()) {
            Log.e("RootManager", "Device architecture not supported")
            setRootModeEnabled(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            Shell.getShell { shell ->
                if (shell.isRoot) {
                    Log.d("RootManager", "Root access granted")
                    startDiagReader()
                } else {
                    Log.e("RootManager", "Root access denied")
                    CoroutineScope(Dispatchers.Main).launch {
                        setRootModeEnabled(false)
                    }
                }
            }
        }
    }

    fun checkRootAccess(): Boolean = Shell.rootAccess()

    private fun isDeviceArchitectureSupported(): Boolean {
        val supportedAbis = Build.SUPPORTED_ABIS
        Log.d("RootManager", "Supported ABIs: ${supportedAbis.joinToString()}")
        return supportedAbis.contains("arm64-v8a")
    }

    private fun startDiagReader() {
        if (diagReaderJob?.isActive == true) return

        val fifoFullPath = "${context.filesDir}/$FIFO_PATH"
        val mkfifoResult = Shell.cmd("su -c 'mkfifo -m 666 \"$fifoFullPath\" 2>/dev/null || true'").exec()
        Log.d("RootManager", "mkfifo result: ${mkfifoResult.out}")

        diagReaderJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val execPath = "${context.filesDir}/$EXEC_NAME"
                copyExecutableFromAssets(execPath)

                val file = File(execPath)
                if (!file.exists() || !file.canExecute()) {
                    Log.e("RootManager", "Executable file problem: $execPath")
                    CoroutineScope(Dispatchers.Main).launch {
                        setRootModeEnabled(false)
                    }
                    return@launch
                }

                val diagCfgPath = "${context.filesDir}/diag.cfg"
                copyDiagCfgFromAssets(diagCfgPath)


                val logFilePath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "diag_log.txt"
                ).absolutePath

                val command = arrayOf(
                    "su",
                    "-c",
                    "exec $execPath $diagCfgPath $fifoFullPath > '$logFilePath' 2>&1"
                )
                Log.d("RootManager", "Executing command: ${command.joinToString(" ")}")

                val process = ProcessBuilder(*command)
                    .start()
                Log.d("RootManager", "diag_revealer started")

                val exitCode = process.waitFor()
                Log.d("RootManager", "diag_revealer exited with code: $exitCode")

            } catch (e: Exception) {
                Log.e("RootManager", "Failed to start diag_revealer: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    setRootModeEnabled(false)
                }
            }
            finally {
                startFifoReader(fifoFullPath)
            }
        }
    }

    private fun copyExecutableFromAssets(destPath: String) {
        try {
            context.assets.open("$EXEC_NAME.so").use { inputStream ->
                val outFile = File(destPath)
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val chmodResult = Shell.cmd("su -c 'chmod 755 $destPath'").exec()
            if (!chmodResult.isSuccess) {
                Log.e("RootManager", "chmod failed: ${chmodResult.err}")
                CoroutineScope(Dispatchers.Main).launch { setRootModeEnabled(false) }
                return
            }
        } catch (e: IOException) {
            Log.e("RootManager", "Failed to copy executable from assets: ${e.message}", e)
            CoroutineScope(Dispatchers.Main).launch {
                setRootModeEnabled(false)
            }
        }
    }

    private fun copyDiagCfgFromAssets(destPath: String) {
        try {
            context.assets.open("diag.cfg").use { inputStream ->
                val outFile = File(destPath)
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("RootManager", "diag.cfg copied successfully to: $destPath")
        } catch (e: IOException) {
            Log.e("RootManager", "Failed to copy diag.cfg from assets: ${e.message}", e)
            CoroutineScope(Dispatchers.Main).launch {
                setRootModeEnabled(false)
            }
        }
    }
    private fun startFifoReader(fifoPath: String) {
        fifoReaderJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val fifoInputStream = FileInputStream(fifoPath)
                Log.d("RootManager", "FIFO opened for reading")

                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (isActive) {
                    bytesRead = fifoInputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOfRange(0, bytesRead)
                        Log.d("FifoReader", "Read ${data.size} bytes from FIFO: ${data.contentToString()}")
                        parseFifoData(data)

                    } else if (bytesRead < 0) {
                        Log.d("FifoReader", "FIFO closed by writer")
                        break
                    }
                }

                fifoInputStream.close()
            } catch (e: IOException) {
                Log.e("FifoReader", "Error reading from FIFO: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch { setRootModeEnabled(false) }
            }
        }
    }

    private fun stopDiagReader() {
        diagReaderJob?.cancel()
        diagReaderJob = null
        fifoReaderJob?.cancel()
        fifoReaderJob = null

        CoroutineScope(Dispatchers.IO).launch {
            val pidResult = Shell.cmd("pidof $EXEC_NAME").exec()
            if (pidResult.isSuccess && pidResult.out.isNotEmpty()) {
                val pid = pidResult.out.first().trim()
                val killResult = Shell.cmd("su -c 'kill -s SIGTERM $pid'").exec()
                Log.d("RootManager", "Sent SIGTERM to diag_revealer (pid=$pid), result: ${killResult.code}")
            }
        }
    }
    private fun parseFifoData(data: ByteArray) {
        var offset = 0
        while (offset + 4 <= data.size) {
            val type = getShort(data, offset)
            val length = getShort(data, offset + 2)

            if (offset + 4 + length > data.size) {
                Log.w("FifoReader", "Incomplete packet, skipping. Expected: ${length + 4}, have: ${data.size - offset}")
                break
            }

            when (type) {
                1 -> { // FIFO_MSG_TYPE_LOG
                    if (offset + 12 > data.size) {
                        Log.w("FifoReader", "Incomplete LOG packet (no timestamp).")
                        break
                    }
                    val timestamp = getDouble(data, offset + 4)
                    val payload = data.copyOfRange(offset + 12, offset + 4 + length)
                    val networkMessage = parseMessage(payload)
                    networkMessage?.let { processNetworkMessage(it) }
                }
                2 -> { // FIFO_MSG_TYPE_START_LOG_FILE
                    val filename = String(data, offset + 4, length)
                    Log.d("FifoReader", "Start log file: $filename")
                }
                3 -> { // FIFO_MSG_TYPE_END_LOG_FILE
                    val filename = String(data, offset + 4, length)
                    Log.d("FifoReader", "End log file: $filename")
                }
                else -> {
                    Log.w("FifoReader", "Unknown message type: $type")
                }
            }
            offset += 4 + length
        }
    }
    private fun parseMessage(data: ByteArray): NetworkMessage? {
        return when {
            isGsmMessage(data) -> parseGsmMessage(data)
            isLteMessage(data) -> parseLteMessage(data)
            isWcdmaMessage(data) -> parseWcdmaMessage(data)
            else -> null
        }
    }

    private fun isGsmMessage(data: ByteArray): Boolean =
        data.size >= 3 && (data[0].toInt() and 0x7F) in 0..10

    private fun isLteMessage(data: ByteArray): Boolean =
        data.size >= 6 && data[0].toInt() and 0xFF <= 26

    private fun isWcdmaMessage(data: ByteArray): Boolean =
        data.size >= 8 && data[0].toInt() and 0xFF in 0..0x89

    private fun parseGsmMessage(data: ByteArray): NetworkMessage.GsmMessage {
        val channelTypeDir = data[0].toInt() and 0xFF
        return NetworkMessage.GsmMessage(
            channelType = channelTypeDir and 0x7F,
            messageLength = data[2].toInt() and 0xFF,
            isUplink = (channelTypeDir and 0x80) == 0x00
        )
    }

    private fun parseLteMessage(data: ByteArray): NetworkMessage.LteMessage {
        return NetworkMessage.LteMessage(
            pci = getShort(data, 4),
            earfcn = getInteger(data, 6),
            signalStrength = data[10].toInt() and 0xFF
        )
    }

    private fun parseWcdmaMessage(data: ByteArray): NetworkMessage.WcdmaMessage {
        return NetworkMessage.WcdmaMessage(
            uarfcn = getShort(data, 4),
            psc = getShort(data, 6),
            signalStrength = data[8].toInt() and 0xFF
        )
    }

    private fun processNetworkMessage(message: NetworkMessage) {
        when (message) {
            is NetworkMessage.GsmMessage -> {
                Log.d("RootManager", "GSM: Channel=${message.channelType}, Length=${message.messageLength}, Uplink=${message.isUplink}")
            }
            is NetworkMessage.LteMessage -> {
                Log.d("RootManager", "LTE: PCI=${message.pci}, EARFCN=${message.earfcn}, Signal=${message.signalStrength}")
            }
            is NetworkMessage.WcdmaMessage -> {
                Log.d("RootManager", "WCDMA: UARFCN=${message.uarfcn}, PSC=${message.psc}, Signal=${message.signalStrength}")
            }
        }
    }
    private fun getShort(array: ByteArray, offset: Int): Int =
        ((array[offset + 1].toInt() and 0xFF) shl 8) or
                (array[offset].toInt() and 0xFF)

    private fun getInteger(array: ByteArray, offset: Int): Int =
        ((array[offset + 3].toInt() and 0xFF) shl 24) or
                ((array[offset + 2].toInt() and 0xFF) shl 16) or
                ((array[offset + 1].toInt() and 0xFF) shl 8) or
                (array[offset].toInt() and 0xFF)

    private fun getDouble(array: ByteArray, offset: Int): Double {
        val longValue = ((array[offset + 7].toLong() and 0xFF) shl 56) or
                ((array[offset + 6].toLong() and 0xFF) shl 48) or
                ((array[offset + 5].toLong() and 0xFF) shl 40) or
                ((array[offset + 4].toLong() and 0xFF) shl 32) or
                ((array[offset + 3].toLong() and 0xFF) shl 24) or
                ((array[offset + 2].toLong() and 0xFF) shl 16) or
                ((array[offset + 1].toLong() and 0xFF) shl 8) or
                (array[offset].toLong() and 0xFF)
        return Double.fromBits(longValue)
    }
}