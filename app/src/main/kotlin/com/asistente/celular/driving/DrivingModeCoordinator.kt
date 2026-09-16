package com.asistente.celular.driving

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.asistente.celular.nlu.driving.DrivingModeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinador central del Modo Conducción (In-Car Companion).
 * Detecta automáticamente la conexión a sistemas de sonido de automóviles por Bluetooth
 * y gestiona el ciclo de vida de la experiencia manos libres al volante.
 */
class DrivingModeCoordinator(
    private val context: Context
) : DrivingModeController {

    private val _isDrivingMode = MutableStateFlow(false)
    val isDrivingMode: StateFlow<Boolean> = _isDrivingMode.asStateFlow()

    private var connectedVehicleName: String? = null
    private var isReceiverRegistered = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            @Suppress("DEPRECATION")
            val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (isCarAudioDevice(device)) {
                        val name = getDeviceNameSafe(device) ?: "Vehículo"
                        connectedVehicleName = name
                        _isDrivingMode.value = true
                        Log.i(TAG, "Conexión vehicular detectada: $name. Modo Conducción activado automáticamente.")
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (isCarAudioDevice(device)) {
                        val name = getDeviceNameSafe(device) ?: "Vehículo"
                        if (connectedVehicleName == name) {
                            connectedVehicleName = null
                            _isDrivingMode.value = false
                            Log.i(TAG, "Desconexión vehicular: $name. Modo Conducción desactivado.")
                        }
                    }
                }
            }
        }
    }

    fun startListening() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        try {
            context.registerReceiver(bluetoothReceiver, filter)
            isReceiverRegistered = true
            Log.i(TAG, "DrivingModeCoordinator escuchando eventos de Bluetooth.")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando receiver de Bluetooth para Modo Conducción", e)
        }
    }

    fun stopListening() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(bluetoothReceiver)
            isReceiverRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Error desregistrando receiver de Bluetooth", e)
        }
    }

    override fun isDrivingModeActive(): Boolean = _isDrivingMode.value

    override fun setDrivingMode(active: Boolean): Boolean {
        _isDrivingMode.value = active
        if (!active) {
            connectedVehicleName = null
        }
        Log.i(TAG, "Modo Conducción cambiado manualmente a: $active")
        return true
    }

    override fun getConnectedVehicleName(): String? = connectedVehicleName

    private fun isCarAudioDevice(device: BluetoothDevice?): Boolean {
        if (device == null) return false
        val devClass = try { device.bluetoothClass } catch (_: SecurityException) { null }
        if (devClass != null) {
            val deviceCategory = devClass.deviceClass
            if (deviceCategory == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO) {
                return true
            }
        }

        val name = getDeviceNameSafe(device)?.lowercase() ?: ""
        return CAR_KEYWORDS.any { name.contains(it) }
    }

    private fun getDeviceNameSafe(device: BluetoothDevice?): String? {
        return try {
            device?.name
        } catch (_: SecurityException) {
            null
        }
    }

    companion object {
        private const val TAG = "DrivingCoordinator"

        private val CAR_KEYWORDS = listOf(
            "car", "auto", "vehiculo", "coche",
            "honda", "toyota", "ford", "chevrolet", "bmw", "audi",
            "mazda", "hyundai", "kia", "nissan", "volkswagen", "seat",
            "sync", "uconnect", "carplay", "android auto", "stereo",
            "pioneer", "sony car", "kenwood", "alpine", "bluetooth carkit"
        )
    }
}
