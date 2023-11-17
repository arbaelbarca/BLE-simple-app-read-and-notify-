package com.lorenzofelletti.simpleblescanner.blescanner.ui

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.viewbinding.library.activity.viewBinding
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lorenzofelletti.simpleblescanner.R
import com.lorenzofelletti.simpleblescanner.blescanner.BleDeviceModel
import com.lorenzofelletti.simpleblescanner.blescanner.adapter.BleScanConnectAdapter
import com.lorenzofelletti.simpleblescanner.blescanner.broadcast.BroadcastReceiverNew
import com.lorenzofelletti.simpleblescanner.blescanner.listener.OnClickItemAdapterListener
import com.lorenzofelletti.simpleblescanner.blescanner.service.BluetoothLeService
import com.lorenzofelletti.simpleblescanner.blescanner.utils.HexString
import com.lorenzofelletti.simpleblescanner.databinding.ActivityDetailScanBleBinding
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.UUID


private const val TAG = "BluetoothLeService"

class DetailScanBleActivity : AppCompatActivity() {


    val activityBinding: ActivityDetailScanBleBinding by viewBinding()
    var getAddressDevice = ""

    var bluetoothService: BluetoothLeService? = null
    lateinit var bleScanConnectAdapter: BleScanConnectAdapter
    lateinit var broadcastReceiver: BroadcastReceiverNew
    lateinit var rxBleClient: RxBleClient

    private lateinit var connectionObservable: Observable<RxBleConnection>
    lateinit var rxBleDevice: RxBleDevice

    var ble_uuid_service_new = "0000ffe1-0000-1000-8000-00805f9b34fb"
    private val discoveryDisposable = CompositeDisposable()
    var bleDeviceModel: BleDeviceModel? = null

//    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                BluetoothLeService.ACTION_GATT_CONNECTED -> {
//                    println("respon Connect")
//                    activityBinding.tvStatusConnect.text = "Connected"
////                    activityBinding.tvStatusConnect.setTextColor(ContextCompat.getColor(this@DetailScanBleActivity,R.))
//                    activityBinding.pbListScan.visibility = View.GONE
//                }
//
//                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
//                    println("respon Disconnected")
//                    activityBinding.tvStatusConnect.text = "Disconnected"
//                    activityBinding.pbListScan.visibility = View.GONE
//                }
//
//                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
//                    println("respon Disconvererd")
//                    displayGattServices(bluetoothService?.getSupportedGattServices() as List<BluetoothGattService>?)
//                    bleScanConnectAdapter.setValueNotify(intent.getStringExtra("test").toString())
//                }
//
//                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
//                    val getDataValue = intent.getStringExtra("data")
//                    println("respon Available $getDataValue")
//                    bleScanConnectAdapter.setValueNotify(getDataValue.toString())
//                }
//            }
//        }
//    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                // perform device connection

                bluetooth.scanConnect(getAddressDevice)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private fun triggerDisconnect() = disconnectTriggerSubject.onNext(Unit)

    private fun prepareConnectionObservable(): Observable<RxBleConnection> =
        rxBleDevice
            .establishConnection(false)
            .takeUntil(disconnectTriggerSubject)


    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private val connectionDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_scan_ble)

        initial()
    }

    private fun initial() {
        rxBleClient = RxBleClient.create(this)
        broadcastReceiver = BroadcastReceiverNew()
        getAddressDevice = intent.getStringExtra("address").toString()
        rxBleDevice = rxBleClient.getBleDevice(getAddressDevice)

        bindServiceIntent()
        initAdapter()
//        connectionBleDevices(getAddressDevice)
        callBleServices()
        activityBinding.btnNotify.setOnClickListener {
//            onNotifyClick(ble_uuid_service_new as UUID)
        }
    }

    private fun initAdapter() {
        bleScanConnectAdapter = BleScanConnectAdapter(
            object : OnClickItemAdapterListener {
                override fun clickItem(pos: Int, any: Any) {
                    val dataItem = any as BleDeviceModel
                    clickItemNotify(dataItem)
                }
            }
        )
        activityBinding.rvListScan.apply {
            adapter = bleScanConnectAdapter
            layoutManager = LinearLayoutManager(this@DetailScanBleActivity)
            hasFixedSize()
        }
    }

    private fun clickItemNotify(bleDeviceModel: BleDeviceModel) {
//        bluetoothService?.setCharacteristicNotification(
//            dataItem.characteristics[0].uuid.toString(),
//            dataItem.characteristics[0],
//            true
//        )
        println("respon Character ${bleDeviceModel.uuidCharacteristic}")
        onNotifyClick(bleDeviceModel.uuidCharacteristic!!)
        Toast.makeText(this, "Success notify characteristic", Toast.LENGTH_SHORT).show()
    }


    override fun onResume() {
        super.onResume()
//        registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter())
//        if (bluetoothService != null) {
//            val result = bluetoothService!!.scanConnect(getAddressDevice)
//            Log.d("respon ", "Connect request result=$result")
//        }
    }

    override fun onPause() {
        super.onPause()
//        unregisterReceiver(broadcastReceiver)
        discoveryDisposable.clear()
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }

    fun bindServiceIntent() {
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    fun connectionBleDevices(macAddress: String) {
        rxBleDevice.establishConnection(false) // <-- autoConnect flag
            ?.subscribe(
                { rxBleConnection: RxBleConnection? ->
                    println("respon connected $rxBleConnection")
                }
            ) { throwable: Throwable? ->
                throwable?.printStackTrace()
            }

//        disposable?.dispose()
    }

    private fun callBleServices() {
        rxBleDevice.establishConnection(false)
            .flatMapSingle { it.discoverServices() }
            .take(1) // Disconnect automatically after discovery
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                updateUi()
            }
            .doFinally { updateUi() }
            .subscribe({
                it.bluetoothGattServices.forEach {
                    bleDeviceModel = BleDeviceModel(
                        it.uuid,
                        it.characteristics[0].uuid,
                        "",
                        "",
                    )
                }
//                it.bluetoothGattServices.flatMap { dataService ->
//                    println("respon DataService ${dataService.characteristics[0].uuid}")
//                    dataService.characteristics.map { dataChart ->
//
//                    }
//
//                }

                bleDeviceModel?.let { it1 -> bleScanConnectAdapter.addListScanConnectRx(it1) }
            }, {
//                showSnackbarShort("Connection error: $it")
                it.printStackTrace()
            })
            .let { discoveryDisposable.add(it) }
    }

    private fun updateUi() {
        activityBinding.tvStatusConnect.text = "Connected"
//        activityBinding.tvStatusConnect.setTextColor(ContextCompat.getColor(this@DetailScanBleActivity, R.))
        activityBinding.pbListScan.visibility = View.GONE
    }

    fun readBleDevices(rxBleDevice: RxBleDevice, rxBleConnection: RxBleConnection) {
//        rxBleDevice.establishConnection(false)
//            .flatMapSingle(rxBleConnection,rxBleConnection.readCharacteristic(ch))
    }

    private fun onReadClick(uuid: UUID) {
        connectionObservable
            .firstOrError()
            .flatMap {
                it.readCharacteristic(uuid)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bytes ->

            }, {

            }
            )
            .let {

            }
    }

    private fun onWriteClick() {
//        connectionObservable
//            .firstOrError()
//            .flatMap { it.writeCharacteristic(characteristicUuid, inputBytes) }
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
//            .let { connectionDisposable.add(it) }
    }

    private fun onNotifyClick(uuid: UUID) {
        rxBleDevice.establishConnection(false)
            .flatMap {
                it.setupNotification(uuid)
            }
            .doOnNext {
                runOnUiThread {
                    println("respon notify has been setup")
                }
            }
            .flatMap { it }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                println("respon get value $it")
                bleScanConnectAdapter.setValueNotify(HexString.bytesToHex(it))
            }, {
                it.printStackTrace()
            })
            .let {
                connectionDisposable.add(it)
            }
    }


}