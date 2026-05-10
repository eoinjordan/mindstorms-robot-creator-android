package com.eoinedge.robotinventor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of [RobotTransport] for LEGO SPIKE Prime / Robot Inventor via BLE.
 */
class SpikeBleTransport(private val context: Context) : RobotTransport {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    @SuppressLint("MissingPermission")
    override suspend fun scan(): List<RobotDevice> {
        if (adapter == null || !adapter.isEnabled) return emptyList()
        
        val scanner = adapter.bluetoothLeScanner ?: return emptyList()
        val foundDevices = mutableListOf<RobotDevice>()
        
        return suspendCancellableCoroutine { continuation ->
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val name = device.name ?: "Unknown Device"
                    if (name.contains("LEGO", ignoreCase = true) || name.contains("Hub", ignoreCase = true)) {
                        foundDevices.add(RobotDevice(
                            id = device.address,
                            name = name,
                            type = "Robot Inventor 51515",
                            connectionState = ConnectionState.DISCONNECTED
                        ))
                    }
                }
            }
            
            scanner.startScan(callback)
            
            // Stop scan after 5 seconds and return results
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                scanner.stopScan(callback)
                if (continuation.isActive) {
                    continuation.resume(foundDevices.distinctBy { it.id })
                }
            }, 5000)

            continuation.invokeOnCancellation {
                scanner.stopScan(callback)
            }
        }
    }

    override suspend fun connect(deviceId: String): RobotConnection {
        // TODO: Implement BLE connection and Pybricks protocol
        delay(500)
        throw UnsupportedOperationException("BLE connection logic (Pybricks/LEGO) is complex and requires specialized protocol implementation.")
    }

    override suspend fun describe(): RobotDescription {
        return RobotDescription(emptyList(), "0.0.0", emptyList())
    }

    override suspend fun runProbe(plan: ProbePlan): Flow<ProbeTelemetry> {
        return emptyFlow()
    }

    override suspend fun stopAll() {
        // TODO: Send stop command via BLE
    }
}
