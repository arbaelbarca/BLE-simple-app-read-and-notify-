package com.lorenzofelletti.simpleblescanner.blescanner.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lorenzofelletti.simpleblescanner.blescanner.service.BluetoothLeService

class BroadcastReceiverNew : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        when (p1?.action) {
            BluetoothLeService.ACTION_GATT_CONNECTED -> {
                println("respon Connect")
            }

            BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                println("respon Disconnected")
            }

            BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                val intent = p1.getStringExtra("test")
                println("respon Disconvererd $intent")

            }

            BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                println("respon Disconvererd")
            }
        }
    }
}