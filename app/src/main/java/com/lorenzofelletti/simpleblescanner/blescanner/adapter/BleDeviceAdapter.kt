package com.lorenzofelletti.simpleblescanner.blescanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lorenzofelletti.simpleblescanner.R
import com.lorenzofelletti.simpleblescanner.blescanner.listener.OnClickItemAdapterListener
import com.lorenzofelletti.simpleblescanner.blescanner.model.BleDevice

/**
 * Adapter for the RecyclerView that shows the found BLE devices.
 */
class BleDeviceAdapter(
    val onClickItemAdapterListener: OnClickItemAdapterListener? = null
) : RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {

    private val devices: MutableList<BleDevice> = mutableListOf()

    fun addList(getDeviceList: MutableList<BleDevice>) {
        devices.clear()
        devices.addAll(getDeviceList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.device_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val deviceView = inflater.inflate(R.layout.device_row_layout, parent, false)
        return ViewHolder(deviceView)
    }

    override fun onBindViewHolder(holder: BleDeviceAdapter.ViewHolder, position: Int) {
        val device = devices[position]
        val textView = holder.deviceNameTextView
        textView.text = device.name + " " + device.address
        holder.itemView.setOnClickListener {
            onClickItemAdapterListener?.clickItem(position, device)
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }
}