package com.example.bluetooth_v2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.NonCancellable.start
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBtLauncher: ActivityResultLauncher<Intent>
    private val discoveredDevices = mutableListOf<Devices>()
    private lateinit var adapter: myAdapter
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null


    companion object{
        val myUUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = myAdapter(discoveredDevices)
        {
            device ->
            connectDevice(device)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT)
                .show()
        }

        enableBtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
                }

            }

        startAcceptThread()

    }


    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET
            )
            val missingPermissions = requiredPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
                return false
            }
        }
        return true
    }


    fun bluetoothOn(v: View) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                enableBtLauncher.launch(enableBluetoothIntent)


            }

        }

    }


    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)



                if(device != null)
                {
                    val deviceName = device.name ?: ""
                    val deviceAddress = device.address ?: ""

                    Log.i("BluetoothDiscovery", "Device: $deviceName")

                    val newDevice = Devices(deviceName, deviceAddress, device)
                    if(!discoveredDevices.contains(newDevice))
                    {
                        discoveredDevices.add(newDevice)
                        Log.i("deviceName", newDevice.toString())
                        adapter.notifyDataSetChanged()
                    }
                }


            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    fun pair(v: View)
    {

        if(!checkPermissions())
        {
            return
        }
        else{
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(receiver, discoverDevicesIntent)
            bluetoothAdapter.startDiscovery()

        }


    }



    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BluetoothApp", myUUID)

        }
        override fun run() {
            Log.i("BluetoothUUID", "Listening for connections with UUID: $myUUID")
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }


        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread(){
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {

            device.createRfcommSocketToServiceRecord(myUUID)
        }

          override fun run(){

              bluetoothAdapter.cancelDiscovery()

              Log.i("BluetoothUUID", "Attempting to connect with UUID: $myUUID")
              try {
                  mmSocket?.connect()
                  manageConnectedSocket(mmSocket!!)


                  Log.i("Bluetooth", "Connected successfully!")
              } catch (e: IOException) {
                  e.printStackTrace()
                  Log.e("Bluetooth", "Connection failed: ${e.message}")

              }
              try {
                  mmSocket?.close()
              } catch (closeException: IOException) {
                  Log.e("Bluetooth", "Could not close the client socket", closeException)
              }
                  return



        }

        fun cancel()
        {
            mmSocket?.close()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: Devices)
    {

        val bDevice = device.bluetoothDevice ?: return

        bluetoothAdapter.cancelDiscovery()
        connectThread?.cancel()

        connectThread = ConnectThread(bDevice)

        connectThread?.start()

        Toast.makeText(this, "The bluetooth device is connected", Toast.LENGTH_SHORT).show()


    }

    fun startAcceptThread()
    {
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    @SuppressLint("MissingPermission")
    fun manageConnectedSocket(socket:BluetoothSocket)
    {
        runOnUiThread{
            Toast.makeText(this, "Connected to ${socket.remoteDevice.name}", Toast.LENGTH_SHORT).show()
            Log.i("Bluetooth", "Connected to ${socket.remoteDevice.name}")




        }

    }



}