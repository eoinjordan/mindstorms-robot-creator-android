package com.eoinedge.robotinventor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * BLE transport for LEGO Wireless Protocol hubs.
 *
 * Android cannot persistently flash arbitrary Python onto stock LEGO firmware over LWP3.
 * This transport runs the supported block/code subset as direct BLE commands: hub light,
 * low-power motor start/stop, and waits. Pybricks/LEGO app program files are still exported
 * for full project transfer.
 */
class SpikeBleTransport(private val context: Context) : RobotTransport, ProgramDeployTransport {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scannedDevices = mutableMapOf<String, BluetoothDevice>()

    @SuppressLint("MissingPermission")
    override suspend fun scan(): List<RobotDevice> {
        if (adapter == null || !adapter.isEnabled) return emptyList()

        val scanner = adapter.bluetoothLeScanner ?: return emptyList()
        val foundDevices = linkedMapOf<String, RobotDevice>()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    recordScanResult(result, foundDevices)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { recordScanResult(it, foundDevices) }
                }
            }

            val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(LWP3_SERVICE_UUID)).build())
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(filters, settings, callback)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                scanner.stopScan(callback)
                if (continuation.isActive) continuation.resume(foundDevices.values.toList())
            }, 5000)

            continuation.invokeOnCancellation {
                scanner.stopScan(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordScanResult(result: ScanResult, foundDevices: MutableMap<String, RobotDevice>) {
        val device = result.device
        val name = result.scanRecord?.deviceName ?: device.name ?: "LEGO Hub"
        val hasLwp3 = result.scanRecord?.serviceUuids?.any { it.uuid == LWP3_SERVICE_UUID } == true
        val looksLikeLego = hasLwp3 ||
            name.contains("LEGO", ignoreCase = true) ||
            name.contains("Hub", ignoreCase = true) ||
            name.contains("WeDo", ignoreCase = true) ||
            name.contains("SPIKE", ignoreCase = true)
        if (!looksLikeLego) return

        scannedDevices[device.address] = device
        foundDevices[device.address] = RobotDevice(
            id = device.address,
            name = name,
            type = classifyHubName(name),
            batteryLevel = null,
            connectionState = ConnectionState.DISCONNECTED
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceId: String): RobotConnection {
        val device = scannedDevices[deviceId] ?: adapter?.getRemoteDevice(deviceId)
            ?: throw IllegalArgumentException("Bluetooth device not found: $deviceId")
        val session = connectLwp3(device) ?: throw IllegalStateException("Could not connect to LEGO BLE service on ${device.name ?: deviceId}")
        return object : RobotConnection {
            override val deviceId: String = deviceId
            override suspend fun disconnect() {
                session.close()
            }
        }
    }

    override suspend fun describe(): RobotDescription {
        return RobotDescription(
            ports = listOf(
                RobotPort("A", "LEGO LWP3 output port"),
                RobotPort("B", "LEGO LWP3 output port")
            ),
            firmwareVersion = "BLE LWP3",
            capabilities = listOf("scan", "connect", "direct BLE run", "stop all", "hub light", "motor A/B")
        )
    }

    override suspend fun runProbe(plan: ProbePlan): Flow<ProbeTelemetry> {
        return emptyFlow()
    }

    override suspend fun stopAll() {
        // stopAll without an active session is intentionally a no-op. deployProgram always
        // ends by sending motor stops before disconnecting.
    }

    @SuppressLint("MissingPermission")
    override suspend fun deployProgram(profile: RobotProfile, code: String): ProgramDeployResult {
        if (profile.family !in DIRECT_BLE_FAMILIES) {
            return ProgramDeployResult(
                ok = false,
                message = "${profile.name} cannot be directly flashed over Android BLE. Use export/handoff for ${profile.family}.",
                deviceName = profile.name
            )
        }
        if (adapter == null || !adapter.isEnabled) {
            return ProgramDeployResult(false, "Bluetooth is off or unavailable on this Android device.")
        }

        val actions = Lwp3ProgramCompiler.compile(code)
        if (actions.isEmpty()) {
            return ProgramDeployResult(false, "No supported Bluetooth commands found. Use light, motor A/B, wait, or stop blocks.")
        }

        val devices = scan()
        val candidate = chooseDevice(profile, devices)
            ?: return ProgramDeployResult(false, "No LEGO BLE hub found. Turn the hub on, pair/allow permissions, then scan again.")
        val device = scannedDevices[candidate.id] ?: adapter.getRemoteDevice(candidate.id)
        val session = connectLwp3(device)
            ?: return ProgramDeployResult(false, "Found ${candidate.name}, but could not open the LEGO BLE command service.", candidate.name)

        return try {
            session.enableNotifications()
            for (action in actions) {
                when (action) {
                    is HubAction.Light -> session.send(Lwp3Commands.setLight(action.colorId))
                    is HubAction.Motor -> session.send(Lwp3Commands.setMotor(action.portId, action.power))
                    is HubAction.Stop -> session.send(Lwp3Commands.setMotor(action.portId, 0))
                    is HubAction.Wait -> delay(action.ms)
                    HubAction.StopAll -> {
                        session.send(Lwp3Commands.setMotor(PORT_A, 0))
                        session.send(Lwp3Commands.setMotor(PORT_B, 0))
                    }
                }
                delay(60)
            }
            session.send(Lwp3Commands.setMotor(PORT_A, 0))
            session.send(Lwp3Commands.setMotor(PORT_B, 0))
            ProgramDeployResult(true, "Sent Bluetooth commands to ${candidate.name}.", candidate.name)
        } catch (e: Exception) {
            ProgramDeployResult(false, "Bluetooth flash/run failed: ${e.message}", candidate.name)
        } finally {
            session.close()
        }
    }

    private fun chooseDevice(profile: RobotProfile, devices: List<RobotDevice>): RobotDevice? {
        val familyNeedle = when (profile.family) {
            "wedo2" -> listOf("wedo", "smart hub", "hub")
            "robot-inventor", "spike-prime" -> listOf("lego", "hub", "spike", "inventor")
            else -> emptyList()
        }
        return devices.firstOrNull { device ->
            familyNeedle.any { needle -> device.name.contains(needle, ignoreCase = true) || device.type.contains(needle, ignoreCase = true) }
        } ?: devices.firstOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectLwp3(device: BluetoothDevice): Lwp3Session? {
        val connectionResult = CompletableDeferred<BluetoothGatt?>()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    if (!connectionResult.isCompleted) connectionResult.complete(null)
                    return
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (!connectionResult.isCompleted) connectionResult.complete(null)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (!connectionResult.isCompleted) connectionResult.complete(gatt)
                } else if (!connectionResult.isCompleted) {
                    connectionResult.complete(null)
                }
            }
        }

        val gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        val readyGatt: BluetoothGatt? = withTimeoutOrNull(10_000) { connectionResult.await() }
        if (readyGatt == null) {
            gatt.close()
            return null
        }
        val service: BluetoothGattService = readyGatt.getService(LWP3_SERVICE_UUID) ?: run {
            readyGatt.close()
            return null
        }
        val characteristic = service.getCharacteristic(LWP3_CHAR_UUID) ?: run {
            readyGatt.close()
            return null
        }
        return Lwp3Session(readyGatt, characteristic)
    }

    private fun classifyHubName(name: String): String = when {
        name.contains("WeDo", ignoreCase = true) -> "WeDo 2.0 Smart Hub"
        name.contains("SPIKE", ignoreCase = true) -> "SPIKE Prime"
        name.contains("Inventor", ignoreCase = true) -> "Robot Inventor 51515"
        else -> "LEGO BLE Hub"
    }

    private class Lwp3Session(
        private val gatt: BluetoothGatt,
        private val characteristic: BluetoothGattCharacteristic
    ) {
        @SuppressLint("MissingPermission")
        fun enableNotifications() {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        @SuppressLint("MissingPermission")
        suspend fun send(bytes: ByteArray) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            } else {
                characteristic.value = bytes
                gatt.writeCharacteristic(characteristic)
            }
            delay(80)
        }

        @SuppressLint("MissingPermission")
        fun close() {
            gatt.disconnect()
            gatt.close()
        }
    }

    private sealed class HubAction {
        data class Light(val colorId: Int) : HubAction()
        data class Motor(val portId: Int, val power: Int) : HubAction()
        data class Stop(val portId: Int) : HubAction()
        data class Wait(val ms: Long) : HubAction()
        data object StopAll : HubAction()
    }

    private object Lwp3ProgramCompiler {
        private val sleepRegex = Regex("""time\.sleep\(([^)]+)\)""")
        private val ledRegex = Regex("""hub\.led\((\d+)\)""")
        private val startRegex = Regex("""motor_([ab])\.start\(speed=([-+]?\d+)""")
        private val stopRegex = Regex("""motor_([ab])\.stop\(\)""")

        fun compile(code: String): List<HubAction> {
            val actions = mutableListOf<HubAction>()
            code.lines().forEach { rawLine ->
                val line = rawLine.trim()
                ledRegex.find(line)?.let {
                    actions += HubAction.Light(it.groupValues[1].toInt().coerceIn(0, 10))
                    return@forEach
                }
                startRegex.find(line)?.let {
                    actions += HubAction.Motor(portId(it.groupValues[1]), it.groupValues[2].toInt().coerceIn(-50, 50))
                    return@forEach
                }
                stopRegex.find(line)?.let {
                    actions += HubAction.Stop(portId(it.groupValues[1]))
                    return@forEach
                }
                sleepRegex.find(line)?.let {
                    val seconds = it.groupValues[1].toDoubleOrNull() ?: 0.2
                    actions += HubAction.Wait((seconds.coerceIn(0.05, 2.0) * 1000).toLong())
                }
            }
            if (actions.any { it is HubAction.Motor }) actions += HubAction.StopAll
            return actions
        }

        private fun portId(port: String): Int = when (port.lowercase(Locale.US)) {
            "a" -> PORT_A
            "b" -> PORT_B
            else -> PORT_A
        }
    }

    private object Lwp3Commands {
        fun setMotor(portId: Int, power: Int): ByteArray {
            val p = power.coerceIn(-50, 50).toByte()
            return byteArrayOf(0x08, 0x00, 0x81.toByte(), portId.toByte(), 0x11, 0x51, 0x00, p)
        }

        fun setLight(colorId: Int): ByteArray {
            return byteArrayOf(0x08, 0x00, 0x81.toByte(), LED_PORT.toByte(), 0x11, 0x51, 0x00, colorId.coerceIn(0, 10).toByte())
        }
    }

    private companion object {
        val LWP3_SERVICE_UUID: UUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")
        val LWP3_CHAR_UUID: UUID = UUID.fromString("00001624-1212-efde-1623-785feabcd123")
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val PORT_A = 0x00
        const val PORT_B = 0x01
        const val LED_PORT = 0x32
        val DIRECT_BLE_FAMILIES = setOf("wedo2", "robot-inventor", "spike-prime")
    }
}
