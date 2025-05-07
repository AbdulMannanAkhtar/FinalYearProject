package com.example.mymotor


import android.bluetooth.BluetoothDevice

data class Devices (
    val dName: String,
    val dAddress: String,
    val bluetoothDevice: BluetoothDevice


)

fun toString(dName:String, dAddress: String) : String
{
    return dName + dAddress
}