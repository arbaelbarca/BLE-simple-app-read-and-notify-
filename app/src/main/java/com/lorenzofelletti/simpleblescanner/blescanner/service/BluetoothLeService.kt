package com.lorenzofelletti.simpleblescanner.blescanner.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import java.util.UUID

private const val TAG = "BluetoothLeService"

class BluetoothLeService : Service() {

    companion object {
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"

        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

        const val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

        val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(HEART_RATE_MEASUREMENT)

    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothGatt: BluetoothGatt? = null

    private val binder = LocalBinder()

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                broadcastUpdate(ACTION_GATT_CONNECTED)
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                println("respon Status $status")
                bluetoothGatt = gatt

            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        //ini untuk read characteristic
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            value: ByteArray,
//            status: Int
//        ) {
//            println("respon Status read $status")
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//            }
//        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            println("respon Status read $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }


        //ini untuk notfy jikalau sudah di enabled
//        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
//            println("respon Status change")
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            println("respon Write descriptro $status")

            gatt?.services?.forEach {
                readCharacteristic(it.characteristics[0], this@BluetoothLeService)
            }

        }
    }

    fun initialize(): Boolean {
        val bluetoothManager: BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun scanConnect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                    Log.d("respon Masuk 13", " ")
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val bluetoothDevice: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)!!
                        bluetoothGatt = bluetoothDevice.connectGatt(this, false, bluetoothGattCallback)
                    } else {
                        Log.d("respon Masuk 13", " else 13 ")
                        return false
                    }
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_ADMIN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                    } else {
                        return false
                    }
                }
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        return false
    }


    private fun broadcastUpdate(action: String) {
        val randomValue = (0..50).random()
        val intent = Intent(action)
        intent.putExtra("test", randomValue.toString())
        sendBroadcast(intent)
    }

    @SuppressLint("MissingPermission")
    fun setCharacteristicNotification(
        uidCharacteristic: String,
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled) ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
        }

        val descriptor =
            getCccd(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)

        if (descriptor != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                println("respon get Masuk os 13")
                bluetoothGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                bluetoothGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                bluetoothGatt?.writeDescriptor(descriptor)
                println("respon get Value Desc ${Gson().toJson(descriptor.value)}")
            }
        }


        // This is specific to Heart Rate Measurement.
//        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
//            val descriptor = characteristic.getDescriptor(
//                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
//            )
//            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//            bluetoothGatt?.writeDescriptor(descriptor)
//        }
//        val descriptor = characteristic.getDescriptor(
//            UUID.fromString(characteristic.descriptors[0].uuid.toString())
//        )

    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                println("respon Read aja os 13")
                bluetoothGatt?.readCharacteristic(characteristic)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                println("respon Read aja os 10")
                bluetoothGatt?.readCharacteristic(characteristic)
            }
        }
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)


        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            var format = -1
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
                Log.d("respon ", "Heart rate format UINT16.")
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
                Log.d("respon ", "Heart rate format UINT8.")
            }
            val heartRate = characteristic.getIntValue(format, 1)
            Log.d("respon ", String.format("Received heart rate: %d", heartRate))
            intent.putExtra("data", heartRate.toString())
        } else {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
                intent.putExtra("data", """${String(data)} $stringBuilder""".trimIndent())
            }
        }
        sendBroadcast(intent)
    }

    private fun getCccd(
        characteristic: BluetoothGattCharacteristic?,
        requiredProperty: Int
    ): BluetoothGattDescriptor? {
        if (characteristic == null) return null

        // Check characteristic property
        val properties = characteristic.properties
        return if (properties and requiredProperty == 0) null else characteristic.getDescriptor(
            CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID
        )
    }
}