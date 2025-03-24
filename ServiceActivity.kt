package com.example.mymotor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Request.*
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.mymotor.HomeActivity.Companion.connectedThread
import com.google.android.gms.location.LocationServices
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri


class ServiceActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private val OBDScans = mutableListOf<Scan>()
    private lateinit var adapter: serviceAdapter
    //private val scanList=  mutableListOf<Scan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service)

        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = serviceAdapter(OBDScans)
        {
            scan ->
            responseDialog(scan)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        intent = Intent()

        //sampleData()


        val getScan: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/scan")

        getScan.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //OBDScans.clear()
                val tempList = mutableListOf<Scan>()

                for (scanSnapshot in snapshot.children) {
                    try {


                        val timestampValue = scanSnapshot.child("timestamp").value


                        val scanClass = scanSnapshot.getValue(Scan::class.java)

                        if (scanClass != null) {
                            scanClass.timestamp = when  (timestampValue) {
                                is Long -> timestampValue
                                is String -> {
                                    val sdf = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault())
                                    try {
                                        sdf.parse(timestampValue)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }
                                }

                                else -> System.currentTimeMillis()

                            }
                            tempList.add(scanClass)


                        }
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error parsing scan: ${e.message}")
                    }
                }

                OBDScans.clear()
                OBDScans.addAll(tempList)
                adapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
            }
        })


    }


    fun newScan(v: View) {


        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()
        var scanReference: DatabaseReference
        //val scanCounter = db.child("scans")

       // scanCounter.get().addOnSuccessListener {
           // snapshot ->
           // var scanID = "001"
           // if(snapshot.exists())
           // {
              //  val lastScan = snapshot.getValue(Int::class.java) ?: 0
               // scanID = String.format("%03d", lastScan + 1)
           // }

            val scanID = db.push().key ?: return
            //val stf = SimpleDateFormat("HH:mm:ss dd:MM:yyyy", Locale.getDefault())
            val timestamp = System.currentTimeMillis()

            //: ConnectedThread

            val socket = HomeActivity.BluetoothSocketHolder.socket

            if(socket == null || !socket.isConnected)
            {
                Toast.makeText(this, "Your not connected to the OBD device", Toast.LENGTH_SHORT).show()
                return//@addOnSuccessListener
            }


            var responses = mutableListOf<String>()


            connectedThread?.messageRecieved = { message ->
                runOnUiThread {
                    Log.i("Bluetooth", "received message: $message")


                    responses.add(message.trim())
                    val descriptions = loadDescriptions(this)

                    val formattedResponse = responses.map {
                        code ->
                        val obdData  = descriptions[code]

                        if(obdData != null)
                        {
                            "$code  -  ${obdData.description}"
                        }
                        else
                        {
                            "$code \n Description: Unknown code "
                        }

                    }.joinToString(" \n ")

                    val scan = Scan(scanID, timestamp, obdResponse = formattedResponse)


                    scanReference = db.child("users").child(userId).child("scan").child(scanID)
                    scanReference.setValue(scan).addOnSuccessListener {

                        Toast.makeText(this, "Scan added", Toast.LENGTH_SHORT).show()

                        val getScan: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/scan")

                        confirmDialog(scan)


                        getScan.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                //OBDScans.clear()
                                val tempList = mutableListOf<Scan>()

                                for (scanSnapshot in snapshot.children) {
                                    try {

                                        val timestampValue = scanSnapshot.child("timestamp").value

                                        val scanClass = scanSnapshot.getValue(Scan::class.java)

                                        if (scanClass != null) {
                                            scanClass.timestamp = when  (timestampValue) {
                                                is Long -> timestampValue
                                                is String -> {
                                                    val sdf = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault())
                                                    try {
                                                        sdf.parse(timestampValue)?.time ?: System.currentTimeMillis()
                                                    } catch (e: Exception) {
                                                        System.currentTimeMillis()
                                                    }
                                                }

                                                else -> System.currentTimeMillis()

                                            }
                                            tempList.add(scanClass)


                                        }
                                    } catch (e: Exception) {
                                        Log.e("Firebase", "Error parsing scan: ${e.message}")
                                    }
                                }

                                OBDScans.clear()
                                OBDScans.addAll(tempList)
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })


                    }
                }

            }
            connectedThread?.sendCommand("03")

        //}

    }

    fun sampleDataBtn(v: View)
    {
        val scan = sampleData()

        if(scan != null)
        {
            confirmDialog(scan)
        }


    }

    fun confirmDialog(scan: Scan)
    {


        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.view_scan, null)

        val build: AlertDialog.Builder =  AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val yes: Button = dialog.findViewById(R.id.yes)
        val no: Button = dialog.findViewById(R.id.no)

        yes.setOnClickListener {
            responseDialog(scan)
            alertDialog.dismiss()
        }

        no.setOnClickListener {
            alertDialog.dismiss()
        }
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

    fun responseDialog(scan: Scan)
    {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.scan_result, null)

        val build: AlertDialog.Builder =  AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val response: ListView = dialog.findViewById(R.id.results)
        val rawResult: TextView = dialog.findViewById(R.id.rawResults)
        val closeButton: Button = dialog.findViewById(R.id.close1)

        val descriptions = loadDescriptions(this)



        val cleanResponse = scan.obdResponse.replace(">", "").trim()

        val rawResponse =  cleanResponse.split(" ").filter()
        {
            it.matches(Regex("^[A-Z0-9]+$"))
        }


        //val codes = rawResponse.drop(1)


        val responseList = rawResponse.drop(1).distinct().map { code ->

           // val paddedCode = code.padStart(4, '0')
            //val formattedCode = formatObdCode(code)
            val obdData = descriptions[code]

            if (obdData != null) {
                "Response code: $code - ${obdData.description}"
                } else {
                    "Response code: $code - Unknown code"
                }

            }

            rawResult.text = "Raw response: $rawResponse"
            //response.text = formattedResponse

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, responseList)
        response.adapter = adapter

        response.setOnItemClickListener{
            _,_, position, _ ->
            val selected = rawResponse.drop(1).distinct()[position]
            val obdData = descriptions[selected]

            if(obdData != null)
            {
                moreInfo(selected, obdData)
            }
        }





        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()



    }

/*
    fun formatObdCode(code: String): String
    {
        if(code.length == 4 && code.matches(Regex("^[0-9A-F]{4}$")))
        {
            val firstDigit = code[0]
            val prefix = when(firstDigit)
            {
                '0', '1', '2', '3' -> "P"
                else -> ""
            }
            return "$prefix$code"
        }
        return code
    }


 */
    fun sampleData(): Scan?
    {
        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser

        if(mUser == null)
        {
            return null
        }
        val userId: String = mUser!!.uid

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("scan")
       // val scanReference = db.child("users").child(userId).child("scan")

        val scanKey = db.push().key ?: return null
        val sampleScan = Scan(scanID = "sampleScan2",
            timestamp = System.currentTimeMillis(),
            obdResponse = "03 | 43 P0440 | >")

        db.child(scanKey).setValue(sampleScan).addOnSuccessListener {

        }
        return sampleScan
    }

    fun moreInfo(code: String, obdData: ObdDescriptions)
    {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.more_info, null)

        val build: AlertDialog.Builder =  AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val title: TextView = dialog.findViewById(R.id.title)
        val explanation: TextView = dialog.findViewById(R.id.explanation)
        val symptoms: TextView = dialog.findViewById(R.id.symptoms)
        val severity: TextView = dialog.findViewById(R.id.severity)
        val close2: Button = dialog.findViewById(R.id.close2)
        val youtube: WebView = dialog.findViewById(R.id.youtubeVideo)
        val garage: ListView = dialog.findViewById(R.id.garages)

        val garageAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ArrayList())
        val garageList = mutableListOf<Map<String, Any>>()
        garage.adapter = garageAdapter

        //val descriptions = loadDescriptions(this)

        title.append("\n Code: $code")


        if(code == "43" || code == "03")
        {
            explanation.text = "Explanation: ${obdData.explanation}"
            symptoms.visibility = View.GONE
            severity.visibility = View.GONE
            youtube.visibility = View.GONE

        }


        explanation.text = "Explanation: ${obdData.explanation}"
        symptoms.text = "Symptoms: ${obdData.symptoms}"
        severity.text =  "Severity: ${obdData.severity}"



        if(obdData.severity == "Urgent")
        {
            severity.setTextColor(resources.getColor(R.color.red))
            severity.append(" - visit your local mechanic")
            youtube.visibility = View.GONE
            if(checkPermissions())
            {
                getUserLocation{
                    lat, lng ->
                    localGarages(lat, lng){
                        garages ->
                        garageList.clear()
                        garageList.addAll(garages)

                        garageAdapter.clear()
                        garageAdapter.addAll(garages.map{
                            "${it["name"]}\n${it["address"]}"
                        })
                        garageAdapter.notifyDataSetChanged()
                    }
                }
            }
            else
            {
                requestLocation()
            }
        }


        if(obdData.severity == "Moderate") {
            severity.setTextColor(resources.getColor(R.color.amber))
            searchVideo(youtube, code)
            garage.visibility = View.GONE
        }


        if(obdData.severity == "No issues")
        {
            severity.setTextColor(resources.getColor(R.color.green))
            youtube.visibility = View.GONE
            garage.visibility = View.GONE
        }

        close2.setOnClickListener {
            alertDialog.dismiss()
        }

        garage.setOnItemClickListener{
            _,_, position, _, ->
            val selectedGarage = garageList[position]
            val name = selectedGarage["name"] as String
            val latitude = selectedGarage["latitude"] as Double
            val longitude = selectedGarage["longitude"] as Double

            val gmmIntentUri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude?q=${Uri.encode(name)}"

            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gmmIntentUri))

            Log.d("MapsDebug", "Intent URI: $gmmIntentUri")


            //mapIntent.setPackage("com.google.android.apps.maps")

            //val chooseMapApp = Intent.createChooser(mapIntent, "open using")

            /*
            if(mapIntent.resolveActivity(packageManager) != null)
            {
                startActivity(mapIntent)
            }
            else
            {
                Toast.makeText(this, "Google maps is not installed on your device", Toast.LENGTH_SHORT).show()
            }

             */

            try{
                startActivity(mapIntent)
            } catch(e: ActivityNotFoundException)
            {
                Log.e("Garages", "No app found to handle maps intent: ${e.message}")
                Toast.makeText(this, "No available maps on your device", Toast.LENGTH_SHORT).show()
            }

        }



    }

    fun searchVideo(youtube: WebView, code: String)
    {
        val apiKey = "AIzaSyCbVbEH3QVfCKt5QDc7AGdQOSQS1mgZRhY"
        val query = "how to fix $code"
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=${query.replace(
            " ", "%20")}&key=$apiKey"

        val queue: RequestQueue = Volley.newRequestQueue(youtube.context)

        val jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val itemsArray = response.getJSONArray("items")
                    if (itemsArray.length() > 0)
                    {
                        val video = itemsArray.getJSONObject(1)
                        val videoId = video.getJSONObject("id").getString("videoId")

                        //val videoUrl = ""
                        youtube.settings.javaScriptEnabled = true
                        youtube.webViewClient = WebViewClient()
                        youtube.loadUrl("https://www.youtube.com/embed/$videoId")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                error ->
                error.printStackTrace()
            })

        queue.add(jsonRequest)
    }


    fun localGarages(lat: Double, lng: Double, callback: (List<Map<String, Any>>) -> Unit)
    {
        val apiKey = "AIzaSyCclscGw43Xt8F8tLSKfsYlaQCkfdxUoC8"
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$lat,$lng&radius=5000&type=car_repair&key=$apiKey"
        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
            response ->
            val results = response.getJSONArray("results")
            val garages = mutableListOf<Map<String, Any>>()

            for(i in 0 until minOf(3, results.length()))
            {
                val garage = results.getJSONObject(i)
                val name = garage.getString("name")
                val address = garage.getString("vicinity")

                val location = garage.getJSONObject("geometry").getJSONObject("location")
                val latitude = location.getDouble("lat")
                val longitude = location.getDouble("lng")

                val garageMap = mapOf(
                    "name" to name,
                    "address" to address,
                    "latitude" to latitude,
                    "longitude" to longitude
                )

                garages.add(garageMap)
            }

            Log.d("Garages", "Fetched garages: $garages")
            callback(garages)
        },
            {
                error ->
                Log.e("Garages", "Error fetching garages: ${error.message}")
                callback(emptyList())

            }
        )

        queue.add(request)
    }

    fun getUserLocation(callback: (Double, Double) -> Unit)
    {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED)
        {
            requestLocation()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener {
            location: Location? ->
            if(location != null)
            {
                callback(location.latitude, location.longitude)
            }

        }

    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
                //Manifest.permission.BLUETOOTH_ADVERTISE
            )
            return requiredPermissions.all {
                ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }


    //request permissions
    private fun requestLocation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                //Manifest.permission.BLUETOOTH_ADVERTISE

            )
            ActivityCompat.requestPermissions(this, requiredPermissions, 1)
        }

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
