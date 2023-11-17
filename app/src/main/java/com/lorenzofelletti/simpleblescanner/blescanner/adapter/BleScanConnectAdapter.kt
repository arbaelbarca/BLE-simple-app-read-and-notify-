package com.lorenzofelletti.simpleblescanner.blescanner.adapter

import android.bluetooth.BluetoothGattService
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lorenzofelletti.simpleblescanner.blescanner.BleDeviceModel
import com.lorenzofelletti.simpleblescanner.blescanner.listener.OnClickItemAdapterListener
import com.lorenzofelletti.simpleblescanner.blescanner.utils.HexString
import com.lorenzofelletti.simpleblescanner.blescanner.utils.ViewBindingVH
import com.lorenzofelletti.simpleblescanner.databinding.LayoutItemScanConnectBinding

class BleScanConnectAdapter(
    val onClickItemAdapterListener: OnClickItemAdapterListener? = null
) : RecyclerView.Adapter<ViewBindingVH>() {

    var getValueNotify = ""

    val listServiceGat: MutableList<BleDeviceModel> = mutableListOf()
    fun addListScanConnect(getListScanService: MutableList<BluetoothGattService>) {
//        listServiceGat.clear()
//        listServiceGat.addAll(getListScanService)
//        notifyDataSetChanged()
    }

    fun addListScanConnectRx(getListScanService: BleDeviceModel) {
        listServiceGat.clear()
        listServiceGat.add(getListScanService)
        notifyDataSetChanged()
    }

    fun setValueNotify(value: String) {
        getValueNotify = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindingVH {
        return ViewBindingVH.create(parent, LayoutItemScanConnectBinding::inflate)
    }

    override fun getItemCount(): Int {
        return listServiceGat.size
    }

    override fun onBindViewHolder(holder: ViewBindingVH, position: Int) {
        val dataItem = listServiceGat[position]
        (holder.binding as LayoutItemScanConnectBinding).apply {
            tvUuidServiceItem.text = dataItem.uuidService.toString()
            tvUuiCharacteristicItem.text = dataItem.uuidCharacteristic.toString()

            imgShowNotif.visibility = View.VISIBLE
            imgShowNotif.setOnClickListener {
                onClickItemAdapterListener?.clickItem(position, dataItem)
            }

            if (getValueNotify.isNotEmpty())
                tvValueNotifyItem.visibility = View.VISIBLE

//            var stringBuilder = StringBuilder()
//
//            stringBuilder.append(String.format("%02X ", getva))
//
//            val d: String = String(data)

            tvValueNotifyItem.text = getValueNotify.decodeHex()
        }
    }

    fun String.decodeHex(): String {
        require(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .toString(Charsets.ISO_8859_1)  // Or whichever encoding your input uses
    }
}