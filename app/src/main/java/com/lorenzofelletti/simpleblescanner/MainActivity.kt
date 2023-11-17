package com.lorenzofelletti.simpleblescanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lorenzofelletti.permissions.PermissionManager
import com.lorenzofelletti.permissions.dispatcher.dsl.*
import com.lorenzofelletti.simpleblescanner.BaseApp.Companion.rxBleClient
import com.lorenzofelletti.simpleblescanner.BuildConfig.DEBUG
import com.lorenzofelletti.simpleblescanner.blescanner.BleScanManager
import com.lorenzofelletti.simpleblescanner.blescanner.adapter.BleDeviceAdapter
import com.lorenzofelletti.simpleblescanner.blescanner.listener.OnClickItemAdapterListener
import com.lorenzofelletti.simpleblescanner.blescanner.model.BleDevice
import com.lorenzofelletti.simpleblescanner.blescanner.model.BleScanCallback
import com.lorenzofelletti.simpleblescanner.blescanner.ui.DetailScanBleActivity
import com.permissionx.guolindev.PermissionX
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable


class MainActivity : AppCompatActivity() {
    private lateinit var btnStartScan: Button

    private lateinit var permissionManager: PermissionManager

    private lateinit var btManager: BluetoothManager
    private lateinit var bleScanManager: BleScanManager

    private lateinit var foundDevices: MutableList<BleDevice>
    lateinit var bleDeviceAdapter: BleDeviceAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionManager = PermissionManager(this)
//        permissionManager buildRequestResultsDispatcher {
//            withRequestCode(BLE_PERMISSION_REQUEST_CODE) {
//                checkPermissions(requestPermission())
//                showRationaleDialog(getString(R.string.ble_permission_rationale))
//                doOnGranted { bleScanManager.scanBleDevices() }
//                doOnDenied {
//                    Toast.makeText(
//                        this@MainActivity,
//                        getString(R.string.ble_permissions_denied_message),
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }

        // RecyclerView handling
        val rvFoundDevices = findViewById<View>(R.id.rv_found_devices) as RecyclerView
        foundDevices = BleDevice.createBleDevicesList()
        bleDeviceAdapter = BleDeviceAdapter(object : OnClickItemAdapterListener {
            override fun clickItem(pos: Int, any: Any) {
                val dataItem = any as BleDevice
                connectDevice(dataItem)
            }
        })
        rvFoundDevices.adapter = bleDeviceAdapter
        rvFoundDevices.layoutManager = LinearLayoutManager(this)

        // BleManager creation
        btManager = getSystemService(BluetoothManager::class.java)
        bleScanManager = BleScanManager(btManager, 5000, scanCallback = BleScanCallback({
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            val name =
                it?.device?.name.toString() + " | " + it?.device?.address
            if (name.isBlank()) return@BleScanCallback
            val device = BleDevice(name, it?.device?.address.toString())

//            foundDevices.add(
//                BleDevice(
//                    name,
//                    it?.device?.address.toString()
//                )
//            )
//            bleDeviceAdapter.addList(foundDevices)

            if (!foundDevices.contains(device)) {
                if (DEBUG) {
                    Log.d(
                        BleScanCallback::class.java.simpleName,
                        "${this.javaClass.enclosingMethod?.name} - Found device: $name"
                    )
                }
                foundDevices.add(device)
                bleDeviceAdapter.notifyItemInserted(foundDevices.size - 1)
                bleDeviceAdapter.addList(foundDevices)
            }
        }))

        // Adding the actions the manager must do before and after scanning
        bleScanManager.beforeScanActions.add { btnStartScan.isEnabled = false }
        bleScanManager.beforeScanActions.add {
            foundDevices.size.let {
                foundDevices.clear()
                bleDeviceAdapter.notifyItemRangeRemoved(0, it)
            }
        }
        bleScanManager.afterScanActions.add { btnStartScan.isEnabled = true }

        // Adding the onclick listener to the start scan button
        btnStartScan = findViewById(R.id.btn_start_scan)
        btnStartScan.setOnClickListener {
//            if (DEBUG) Log.i(TAG, "${it.javaClass.simpleName}:${it.id} - onClick event")

            // Checks if the required permissions are granted and starts the scan if so, otherwise it requests them
//            permissionManager checkRequestAndDispatch BLE_PERMISSION_REQUEST_CODE
            requestPermissionRx()
//            requestPermissionDexter()
        }
    }

    private fun connectDevice(dataItem: BleDevice) {
        startActivity(
            Intent(this, DetailScanBleActivity::class.java)
                .putExtra("address", dataItem.address)
        )
    }


    @SuppressLint("CheckResult")
    private fun requestPermissionRx() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {//di bawah os 13
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                )
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
//                        bleScanManager.scanBleDevices()
                        scanBleDevices()
//                        Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                    } else {
//                        Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                    }
                }
        } else { //di atas os 13
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
//                        bleScanManager.scanBleDevices()
                        scanBleDevices()
                        Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                        println("respon Permission $deniedList")
                    }
                }
        }
    }

    private fun scanBleDevices() {
//        observingState()
        rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .build()
        ).subscribe(
            { scanResult ->
//                println("respon Dpat $scanResult")
                if (scanResult.bleDevice.name != null)
                    foundDevices.add(
                        BleDevice(
                            scanResult.bleDevice.name.toString(),
                            scanResult.bleDevice.macAddress
                        )
                    )

                bleDeviceAdapter.addList(foundDevices)

            }
        ) { throwable ->
            throwable.printStackTrace()
        }

//        scanSubscription.dispose()
    }

    fun observingState() {
        val flowDisposable = rxBleClient.observeStateChanges()
            .switchMap<Any> { state: RxBleClient.State? ->
                when (state) {
                    RxBleClient.State.READY ->                 // everything should work
                        return@switchMap rxBleClient.scanBleDevices()

                    RxBleClient.State.BLUETOOTH_NOT_AVAILABLE, RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED,
                    RxBleClient.State.BLUETOOTH_NOT_ENABLED,
                    RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> return@switchMap Observable.empty()

                    else -> return@switchMap Observable.empty()
                }
            }
            .subscribe(
                { rxBleScanResult: Any? ->
                    println("respon Succes ready $rxBleScanResult")
                }
            ) { throwable: Throwable? ->
                throwable?.printStackTrace()
            }


        flowDisposable.dispose()
    }

    /**
     * Function that checks whether the permission was granted or not
     */
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        kotlin.runCatching {
            permissionManager.dispatchOnRequestPermissionsResult(requestCode, grantResults)
        }.onFailure {
            it.printStackTrace()
        }
    }


    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val BLE_PERMISSION_REQUEST_CODE = 1

        @RequiresApi(Build.VERSION_CODES.S)


        fun requestPermission(): Array<String> {
            val blePermission: Array<String>
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {//di bawah os 13
                blePermission = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                )
            } else blePermission = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            return blePermission
        }
    }

}