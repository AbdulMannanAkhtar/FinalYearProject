package com.example.mymotor


import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import com.example.bluetooth_v3.Devices


class myAdapter (private val devices: List<Devices>,
                 private val selectDevice: (Devices) -> Unit):
    RecyclerView.Adapter<myAdapter.myViewHolder>() {

    class myViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.deviceAddress)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rowlayout1, parent, false)

        return myViewHolder(view)

    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        val device = devices[position]

        holder.deviceName.text = device.dName
        holder.deviceAddress.text = device.dAddress

        holder.itemView.setOnClickListener {
            selectDevice(device)
        }

    }

    override fun getItemCount(): Int = devices.size
}

