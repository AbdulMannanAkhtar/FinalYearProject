package com.example.mymotor

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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null
private var  imageUri: Uri? = null
private lateinit var bluetoothAdapter: BluetoothAdapter
private lateinit var enableBtLauncher: ActivityResultLauncher<Intent>
private val discoveredDevices = mutableListOf<Devices>()
private lateinit var adapter: myAdapter



class HomeActivity : AppCompatActivity() {
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null


    companion object{
        var connectedThread: ConnectedThread? = null
        val myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT)
                .show()
            return
        }



        enableBtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                    startAcceptThread()
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
                }

            }

        if (!checkPermissions()) {
            requestPermissions()
        }
        else{
            startAcceptThread()
        }


        intent = Intent()

        mAuth = FirebaseAuth.getInstance()

        val db: DatabaseReference
        val userId = mAuth.currentUser?.uid

        if (userId != null) {
            obdDashboard(userId)
        }

        db = FirebaseDatabase.getInstance().getReference("users/$userId/car")

        val carDetails = findViewById<TextView>(R.id.carDetails)

        db.addValueEventListener(object: ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                val make = snapshot.child("make").getValue(String::class.java)
                val model = snapshot.child("model").getValue(String::class.java)
                val year = snapshot.child("year").getValue(Int::class.java)

                val car = "$make\n$model\n$year"
                carDetails.text = car
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })



        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.homeButton


        bottomNav.setOnItemSelectedListener {
            menuItem ->
            when(menuItem.itemId)
            {
                R.id.serviceButton -> {
                    val intent = Intent(this, ServiceActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.homeButton -> {
                    if(this !is HomeActivity)
                    {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                    }
                    true
                }
                R.id.journeyButton -> {
                    val intent = Intent(this, JourneyActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.scheduleButton -> {
                    val intent = Intent(this, ServiceActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }

        }





    }

//check permissions during runtime
    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET
                //Manifest.permission.BLUETOOTH_ADVERTISE
            )
            return requiredPermissions.all {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }


//request permissions
    private fun requestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                //Manifest.permission.BLUETOOTH_ADVERTISE
                Manifest.permission.INTERNET
            )
            ActivityCompat.requestPermissions(this, requiredPermissions, 1)
        }
        return true
    }

//Broadcast bluetooth and look for other broadcasts
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

//Server thread to look for other bluetooth devices
    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null

        override fun run() {
            try {
                mmServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothApp", myUUID)
                Log.i("Bluetooth", "Server socket listening")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error creating server socket: ${e.message}", e)
                return
            }

            while (true) {
                try {
                    val socket: BluetoothSocket? = mmServerSocket?.accept()
                    if (socket != null) {
                        Log.i("Bluetooth", "Connection accepted!")
                        manageConnectedSocket(socket)
                        break
                    }
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Socket accept() failed: ${e.message}", e)
                    break
                }
            }
        }



        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


    //Client thread to connect to OBD2
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread(){
        private var mmSocket: BluetoothSocket? =null

        init{
            try{
                mmSocket = device.createRfcommSocketToServiceRecord(myUUID)
            }catch(e: IOException){
                Log.e("Bluetooth", "could not create client socket", e)
            }
        }

        override fun run(){

            bluetoothAdapter.cancelDiscovery()

            Log.i("BluetoothUUID", "Attempting to connect with UUID: $myUUID")
            try {
                mmSocket?.connect()
                if(mmSocket != null)
                {
                    manageConnectedSocket(mmSocket!!)
                    Log.i("Bluetooth", "connected")
                }
                else{
                    Log.i("Bluetooth", "not connected")
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Connection failed: ${e.message}")

                try {
                    mmSocket?.close()
                } catch (closeException: IOException) {
                    Log.e("Bluetooth", "Could not close the client socket", closeException)
                }

            }
        }

        fun cancel()
        {
            mmSocket?.close()
        }
    }

    //Dialog for bluetooth devices RCV
    @SuppressLint("MissingPermission")
    fun bluetooth(v: View)
    {
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            //!= PackageManager.PERMISSION_GRANTED)
            enableBtLauncher.launch(enableBluetoothIntent)
        }

        else {
            startAcceptThread()
        }



        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.connect_bluetooth, null)

        val build: AlertDialog.Builder =  AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val recyclerView: RecyclerView = dialog.findViewById(R.id.recyclerView)
        val closeButton: Button = dialog.findViewById(R.id.close)

        adapter = myAdapter(discoveredDevices)
        {
                device ->
            connectDevice(device)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter


        val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, discoverDevicesIntent)
        bluetoothAdapter.startDiscovery()

        closeButton.setOnClickListener {
            alertDialog.dismiss()

        }


    }
//look for bluetooth devices
    @SuppressLint("MissingPermission")
    fun scan(v: View)
    {
        val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, discoverDevicesIntent)
        bluetoothAdapter.startDiscovery()
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
        acceptThread?.cancel()
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    object BluetoothSocketHolder {
        var socket: BluetoothSocket? = null
    }

    @SuppressLint("MissingPermission")
    fun manageConnectedSocket(socket:BluetoothSocket)
    {
        runOnUiThread{

            Toast.makeText(this, "Connected to ${socket.remoteDevice.name}", Toast.LENGTH_SHORT).show()

            Log.i("Bluetooth", "Connected to ${socket.remoteDevice.name}")

            val bluetoothButton:Button = findViewById(R.id.bluetoothConnect)
            bluetoothButton.setBackgroundColor(resources.getColor(R.color.bluetooth_blue))


            BluetoothSocketHolder.socket = socket


            connectedThread = ConnectedThread(socket){
                    message ->

                Log.i("Bluetooth", "recieved message:  $message")
            }



            connectedThread?.start()

            //val intent = Intent(this, communcation::class.java)
            //startActivity(intent)

            acceptThread?.cancel()

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        acceptThread?.cancel()
        unregisterReceiver(receiver)
        connectThread?.cancel()
        connectedThread?.cancel()
    }

    fun loadDescriptions(context: Context): Map<String, ObdDescriptions> {
        val jsonString: String

        val inputStream = context.assets.open("dtcResponses.json")
        jsonString = inputStream.bufferedReader().use {
            it.readText()
        }

        val type = object : TypeToken<Map<String, ObdDescriptions>>(){}.type

        return Gson().fromJson(jsonString, type)

    }

    fun getLatestScan(userId: String, callback: (Scan?) -> Unit)
    {
        //val urgentCodes: TextView = findViewById(R.id.urgentCodes)

        mAuth = FirebaseAuth.getInstance()

        val db: DatabaseReference
        //userId = mAuth.currentUser?.uid

        db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("scan")

        db.orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    for(scanSnapshot in snapshot.children )
                    {
                        val scanObj = scanSnapshot.getValue(Scan::class.java)

                        callback(scanObj)
                        return
                    }
                    callback(null)
                }



            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }

    fun parseCodes(context: Context, rawCode: String): List<String> {
        //val jsonString = loadDescriptions(context) ?: return emptyList()
        val obdJson = loadDescriptions(context) ?: return emptyList()
        val codes = rawCode.split(" ")
        val urgentSeverity = mutableListOf<String>()

        Log.d("Parsing", "Raw codes received: $codes")

        for (c in codes)
        {
            if (obdJson.containsKey(c))
            {
               // val codeObject = obdJson.getJSONObject(c)
                val severity = obdJson[c]?.severity ?: ""
                Log.d("Parsing", "Code: $c, Severity: $severity")

                if (severity == "Urgent")
                {
                    urgentSeverity.add(c)
                }

            }
        }

        return urgentSeverity
    }

    fun obdDashboard(userId: String)
    {
        getLatestScan(userId){
            scan ->
            val urgentCodesTextView: TextView = findViewById(R.id.urgentCodes)




            if(scan != null)
            {
                val urgentCodes = parseCodes(applicationContext, scan.obdResponse)
                val count = urgentCodes.size
                runOnUiThread {
                    if(count > 0)
                    {
                        val message = "$count Urgent trouble codes found in latest scan " + urgentCodes.toString()

                        urgentCodesTextView.text = message

                        //val view: TextView = findViewById(R.id.viewCodes)



                    }
                    else
                    {
                        val message = "There were no urgent trouble codes that need immediate attention in the latest scan"
                        urgentCodesTextView.text = message
                    }
                }

            }
        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    fun reports(menuItem: MenuItem)
    {
        val intent = Intent(this, ReportActivity::class.java)
        startActivity(intent)
    }

    fun weeklyReport(menuItem: MenuItem)
    {
        val intent = Intent(this, BehaviourReportActivity::class.java)
        startActivity(intent)
    }

    fun service(v: View)
    {
        val intent = Intent(this, ServiceActivity::class.java)
        startActivity(intent)
    }

    fun home(v: View)
    {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    fun journey(v: View)
    {
        val intent = Intent(this, JourneyActivity::class.java)
        startActivity(intent)
    }




}
