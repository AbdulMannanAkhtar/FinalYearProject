package com.example.mymotor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.health.connect.datatypes.units.Percentage
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymotor.HomeActivity.Companion.connectedThread
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import journeyAdapter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.logging.Handler
import kotlin.math.abs
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView


class JourneyActivity : AppCompatActivity() {


    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private val journeys = mutableListOf<Journey>()
    private lateinit var adapter: journeyAdapter
    //private val scanList=  mutableListOf<Scan>()
    private val interval = 1000L
    private val treshold = 3.0f
    private val noise = 0.5f

    private var prevSpeed: Float? = null
    private var prevTime: Long? = null

    private  val behaviourHandler = android.os.Handler(Looper.getMainLooper())
    private var behaviourRunnable: Runnable? = null

    private var currentJourney: Journey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journey)

        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = journeyAdapter(journeys)
        {
            journey ->
            moreInfo(journey)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        intent = Intent()

        //sampleJourney()


        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.reminder, null)

        val build: AlertDialog.Builder = AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val reminder: TextView = dialog.findViewById(R.id.reminder1)
        val close: Button = dialog.findViewById(R.id.close3)

        close.setOnClickListener {
            alertDialog.dismiss()
        }



        val getJourney: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/journey")

        getJourney.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //OBDScans.clear()
                val tempList = mutableListOf<Journey>()

                for (scanSnapshot in snapshot.children) {

                    val journeyClass = scanSnapshot.getValue(Journey::class.java)

                    if (journeyClass != null) {

                        tempList.add(journeyClass)
                    }


                }

                journeys.clear()
                journeys.addAll(tempList)
                adapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
            }
        })

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.journeyButton

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
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.journeyButton -> {
                    if(this !is JourneyActivity)
                    {
                        val intent = Intent(this, JourneyActivity::class.java)
                        startActivity(intent)
                    }
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

    fun sampleJourney(): List<Journey>? {
        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser

        if (mUser == null) {
            return null
        }
        val userId: String = mUser!!.uid

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey")

        val now = System.currentTimeMillis()

        val aggressiveJourney = Journey(
            id = "1", name = "Speed Run", to = "Wexford Dunnes", from = "Home", timestamp = now, startFuel = 50.0f,
            endFuel = 45.0f, usedFuel = 5.0f, distance = "25.4", timeTaken = "00:25:00", endTime  = now + 25 * 60 * 1000,
            avgSpeed = 61.0f, totalAccelerations = 42, aggressiveAccelerations = 28, totalBrakings = 38, aggressiveBrakings = 24,
            drivingStyle = "aggressive", accTime = 15.2f, brakeTime = 12.8f
        )

        val calmJourney = Journey(
            id = "2", name = "Cruising", to = "Town", from = "Home", timestamp = now + 60 * 60 * 1000, startFuel = 30.0f,
            endFuel = 29.6f, usedFuel = 0.4f, distance = "5.2", timeTaken = "00:15:00", endTime = now + 75 * 60 * 1000,
            avgSpeed = 21.0f, totalAccelerations= 12, aggressiveAccelerations = 1, totalBrakings = 10, aggressiveBrakings = 0,
            drivingStyle = "calm", accTime = 4.5f, brakeTime  = 3.2f
        )

        listOf(aggressiveJourney, calmJourney).forEach {
            journey ->
            val key = db.push().key
            if(key != null)
            {
                db.child(key).setValue(journey)
            }
        }

        return listOf(aggressiveJourney, calmJourney)
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
    private fun requestLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                //Manifest.permission.BLUETOOTH_ADVERTISE

            )
            ActivityCompat.requestPermissions(this, requiredPermissions, 1)
        }

    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Double, Double) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener {
            loc ->
            loc?. let{
                callback(it.latitude, it.longitude)
            }
        }

    }

    fun getFuelLevel(callback: (fuelPercentage: Float) -> Unit) {

        val socket = HomeActivity.BluetoothSocketHolder.socket

        if (socket == null || !socket.isConnected) {
            Toast.makeText(this, "Your not connected to the OBD device", Toast.LENGTH_SHORT).show()
            callback(0f)
            return//@addOnSuccessListener
        }



        connectedThread?.messageRecieved = { message ->
            runOnUiThread {
                //Log.i("Bluetooth", "received message: $message")

                val cleanMessage = message.trim()
                val parts = cleanMessage.split(" ")

                if (parts.size >= 3 && parts[0] == "41" && parts[1] == "2F") {
                    val fuelHex = parts[2]
                    val fuelValue = fuelHex.toInt(16)

                    val fuelPercentage = fuelValue * 100 / 255f

                    callback(fuelPercentage)
                } else {
                    callback(0f)
                }


            }

        }
        connectedThread?.sendCommand("012F")
    }

    fun convertPercentageToLiters(percentage: Float, tankCapacity: Float = 53.00f): Float
    {
        return (percentage / 100f) * tankCapacity
    }

    fun getSpeed(callback: (speedKmh: Float) -> Unit) {

        val socket = HomeActivity.BluetoothSocketHolder.socket

        if (socket == null || !socket.isConnected) {
            Toast.makeText(this, "Your not connected to the OBD device", Toast.LENGTH_SHORT).show()
            callback(0f)
            return//@addOnSuccessListener
        }

        connectedThread?.messageRecieved = { message ->
            runOnUiThread {
                //Log.i("Bluetooth", "received message: $message")

                val cleanMessage = message.trim()
                val parts = cleanMessage.split(" ")

                if (parts.size >= 3 && parts[0] == "41" && parts[1] == "0D") {
                    val speedHex = parts[2]
                    val speedKmh = speedHex.toInt(16).toFloat()


                    callback(speedKmh)
                } else {
                    callback(0f)
                }


            }

        }
        connectedThread?.sendCommand("010D")
    }


    fun calcDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float
    {
        val startLocation = Location("start")
        startLocation.latitude = startLat
        startLocation.longitude = startLng

        val endLocation = Location("end")
        endLocation.latitude = endLat
        endLocation.longitude = endLng

        return startLocation.distanceTo(endLocation)

    }

    fun saveToFirebase(journey: Journey)
    {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey").child(journey.id)
            .setValue(journey).addOnSuccessListener {
                Toast.makeText(this, "Journey ended", Toast.LENGTH_SHORT).show()
            }

    }

    fun driverBehaviour(journey: Journey)
    {
        currentJourney = journey

        behaviourRunnable?.let{
            behaviourHandler.removeCallbacks(it)
        }

        prevSpeed = null
        prevTime = null
        //accEvent = false
        //brakeEvent = false

        var prevAccelState = 0

        val minSpeed = 0f

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        behaviourRunnable = object : Runnable {
            override fun run() {
                getSpeed { currentSpeedKmh ->
                    Log.d("DriverBehavior", "Current speed: $currentSpeedKmh km/h")
                    val currentTime = System.currentTimeMillis()


                    if(prevSpeed != null && prevTime != null)
                    {

                            val delta = (currentTime - prevTime!!) / 1000f

                            if (delta > 0f && prevSpeed!! > minSpeed && currentSpeedKmh > minSpeed) {
                                //Log.d("DriverBehavior", "old=${oldSpeed} km/h new=${currentSpeedKmh} km/h dt=${"%.2f".format(delta)} s")
                                val currentSpeedMs = currentSpeedKmh * 0.27778f
                                val prevSpeedMs = prevSpeed!! * 0.27778f
                                val acceleration = (currentSpeedMs - prevSpeedMs) / delta
                                Log.d("DriverBehavior", "Computed acceleration: $acceleration m/s²")

                                var currentState = 0;


                                if (acceleration > noise)
                                {
                                    Log.d("DriverBehavior", "  ▶ counted ACC burst")
                                    currentState = 1
                                }

                                else if (acceleration < -noise)
                                {
                                    Log.d("DriverBehavior", "  ▶ counted BRK burst")
                                    currentState = -1
                                }

                                else
                                {
                                    currentState = 0
                                }

                                if(currentState == 1)
                                {
                                    journey.accTime += delta
                                }

                                if(currentState == -1)
                                {
                                    journey.brakeTime += delta
                                }

                                if (currentState == 1 && prevAccelState != 1)
                                {
                                    Log.d("DriverBehavior", "Counting an acceleration burst at $acceleration")
                                    //accEvent = true
                                    journey.totalAccelerations++

                                    if (acceleration > treshold)
                                    {
                                        journey.aggressiveAccelerations++
                                    }

                                } else if (currentState == -1 && prevAccelState != -1)
                                {
                                    //brakeEvent = true
                                    journey.totalBrakings++
                                    if (acceleration < -treshold)
                                    {
                                        journey.aggressiveBrakings++
                                    }
                                }

                                prevAccelState = currentState
                            }
                    }

                    prevSpeed = currentSpeedKmh
                    prevTime = currentTime
                    behaviourHandler.postDelayed(this, interval)
                }
            }
        }

        behaviourHandler.post(behaviourRunnable!!)
    }





    fun trackJourney(v: View) {

        val socket = HomeActivity.BluetoothSocketHolder.socket

        if (socket == null || !socket.isConnected) {
            Toast.makeText(this, "Your not connected to the OBD device", Toast.LENGTH_SHORT).show()
            return//@addOnSuccessListener
        }
        
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.new_journey, null)

        val build: AlertDialog.Builder = AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val journeyName: EditText = dialog.findViewById(R.id.journeyName)
        val to: EditText = dialog.findViewById(R.id.to)
        val from: EditText = dialog.findViewById(R.id.from)


        val startJourney: Button = dialog.findViewById(R.id.startJourney)

        startJourney.setOnClickListener {
            getCurrentLocation { lat, lon ->

                getFuelLevel { fuelPercentage ->
                    val tankCapacity = 53.00f
                    val startFuelLiters = convertPercentageToLiters(fuelPercentage, tankCapacity)

                    mUser = FirebaseAuth.getInstance().currentUser
                    val userId: String = mUser!!.uid

                    val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()
                    var journeyReference: DatabaseReference

                    val journeyNameString: String = journeyName.text.toString()
                    val StringTo: String = to.text.toString()
                    val fromString: String = from.text.toString()

                    val journeyID = db.push().key ?: return@getFuelLevel
                    val journeyName = journeyNameString
                    val timestamp = System.currentTimeMillis()

                    val journey = Journey(
                        journeyID, journeyName, StringTo, fromString, timestamp,
                        lat, lon, startFuelLiters)

                    driverBehaviour(journey)

                    journey(journey)

                    alertDialog.dismiss()
                }

            }
        }


    }

    fun journey(journey: Journey)
    {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.in_progress_journey, null)

        val build: AlertDialog.Builder = AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val title: TextView = dialog.findViewById(R.id.title)
        val endJourney: Button = dialog.findViewById(R.id.endJourney)
        endJourney.isEnabled = false

        title.append("" + journey.name)

        android.os.Handler(Looper.getMainLooper()).postDelayed(
            {
                endJourney.isEnabled = true
            }, interval * 3)


        endJourney.setOnClickListener {
            //Toast.makeText(this, "you pressed the end button", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()

            end(journey)
        }

    }


    fun end(journey: Journey)
    {

        var saved = false

        android.os.Handler(Looper.getMainLooper()).postDelayed(
            {
                if(!saved)
                {
                    journey.distance = "0.0"
                    journey.avgSpeed = 0f
                    saveToFirebase(journey)
                    saved = true
                }
            }, 3000)

        behaviourRunnable?.let{
            behaviourHandler.removeCallbacks(it)
        }


        getCurrentLocation{
            lat, lon ->
            journey.endLat = lat
            journey.endLng = lon

            val endTime = System.currentTimeMillis()
            journey.endTime = endTime

            val journeyTimeMillis = journey.endTime - journey.timestamp
            val totalSec = journeyTimeMillis / 1000
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            journey.timeTaken = "%dh %dm %ds".format(hours, minutes, seconds)


            val distanceMeters = calcDistance(journey.startLat, journey.startLng, journey.endLat, journey.endLng)
            val distanceKm = distanceMeters / 1000f
            journey.distance = "%.2f".format(distanceKm)

            val durationHours = journeyTimeMillis.toFloat() / (1000f * 60f * 60f)
            if(durationHours > 0f)
            {
                journey.avgSpeed = distanceKm / durationHours
            } else
            {
                journey.avgSpeed = 0f
            }

            journey.avgSpeed = String.format("%.1f", journey.avgSpeed).toFloat()

            val totalAccBrake = journey.totalAccelerations + journey.totalBrakings
            val totalAggressiveAccBrake = journey.aggressiveAccelerations + journey.aggressiveBrakings

            if(totalAccBrake > 0 && totalAggressiveAccBrake.toFloat() / totalAccBrake > 0.5f)
            {
                journey.drivingStyle = "Aggressive"

            }
            else
            {
                journey.drivingStyle = "Calm"

            }

            getFuelLevel { fuelPercentage ->
                val tankCapacity = 53.00f
                val endFuelLiters = convertPercentageToLiters(fuelPercentage, tankCapacity)
                journey.endFuel = endFuelLiters

                journey.usedFuel = journey.startFuel - endFuelLiters



                saveToFirebase(journey)

            }
            saved = true

        }

    }




    fun deleteJourney(journey: Journey)
    {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey")

        db.child(journey.id).removeValue().addOnSuccessListener {

            val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey")

            dbRef.addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tempList = mutableListOf<Journey>()
                    for(scanSnapshot in snapshot.children)
                    {
                        val journeyObj = scanSnapshot.getValue(Journey::class.java)
                        if(journeyObj != null)
                        {
                            tempList.add(journeyObj)
                        }
                    }

                    journeys.clear()
                    journeys.addAll(tempList)
                    adapter.notifyDataSetChanged()

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }

    }

    fun moreInfo(journey: Journey)
    {

        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.journey_more_info, null)


        val journeyName: TextView = dialog.findViewById(R.id.journey)
        val travelled: TextView = dialog.findViewById(R.id.travelled)
        val fuel: TextView = dialog.findViewById(R.id.fuel)
        val distance: TextView = dialog.findViewById(R.id.distance)
        val avgSpeed: TextView = dialog.findViewById(R.id.avgSpeed)
        val drivingStyle: TextView = dialog.findViewById(R.id.drivingStyle)
        val deleteButton: Button = dialog.findViewById(R.id.Delete2)
        val closeButton: Button = dialog.findViewById(R.id.close4)
        val stats: TextView = dialog.findViewById(R.id.stats)

        val graphs: TextView = dialog.findViewById(R.id.moreInfo)

        journeyName.text = journey.name
        travelled.text = journey.from + " to " + journey.to
        fuel.text = "You consumed: " + journey.usedFuel + " liters of fuel"
        distance.text = "You travelled: " + journey.distance + "Kms"
        avgSpeed.text = "Your average speed was: ${String.format("%.1f", journey.avgSpeed)} Km/h"
        drivingStyle.text = "Your driving style was: " + journey.drivingStyle




        val statsText = """
            You had: 
            ${journey.totalAccelerations} Accelerations
            ${journey.totalBrakings} Brakings
            
            Of those: 
            ${journey.aggressiveAccelerations} were aggressive accelerations
            ${journey.aggressiveBrakings} were aggressive brakings
            
        """.trimIndent()

        stats.text = statsText



        if(journey.drivingStyle == "Aggressive")
        {
            val infoBlock  = dialog.findViewById<TextView>(R.id.drivingStyle)
            val colorRes =  R.color.red
            infoBlock.setBackgroundColor(ContextCompat.getColor(this, colorRes))


        }
        else
        {
            val infoBlock = dialog.findViewById<TextView>(R.id.drivingStyle)
            val colorRes =  R.color.green
            infoBlock.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }

        val build: AlertDialog.Builder = AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()


        deleteButton.setOnClickListener {
            deleteJourney(journey)
            alertDialog.dismiss()
        }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        graphs.setOnClickListener {
            val inflater2: LayoutInflater = LayoutInflater.from(this)
            val dialog2: View = inflater2.inflate(R.layout.journey_graphs, null)


            val journeyName2: TextView = dialog2.findViewById(R.id.journeyName)
            val behaviourGraph = dialog2.findViewById<PieChart>(R.id.behaviourGraph)
            val timingGraph = dialog2.findViewById<PieChart>(R.id.timingsGraph)
            val closeButton2: Button = dialog2.findViewById(R.id.close5)


            val build2: AlertDialog.Builder = AlertDialog.Builder(this)
            build2.setView(dialog2)

            val alertDialog2 = build2.create()


            journeyName2.text = journey.name

            val entries = listOf(
                PieEntry(journey.totalAccelerations.toFloat(), "Total accelerations"),
                PieEntry(journey.totalBrakings.toFloat(), "Total brakings"),
                PieEntry(journey.aggressiveAccelerations.toFloat(), "Aggressive accelerations"),
                PieEntry(journey.aggressiveBrakings.toFloat(), "Aggressive brakings"))

            val data = PieDataSet(entries, "Driver Behaviour").apply{
                colors = listOf(
                    ContextCompat.getColor(this@JourneyActivity, R.color.bluetooth_blue),
                    ContextCompat.getColor(this@JourneyActivity, R.color.amber),
                    ContextCompat.getColor(this@JourneyActivity, R.color.green),
                    ContextCompat.getColor(this@JourneyActivity, R.color.blue_purple)

                )

                sliceSpace = 3f
                valueTextSize = 10f
                valueTextColor = R.color.black
                setValueFormatter(DefaultValueFormatter(0))
            }

            behaviourGraph.data = PieData(data)

            behaviourGraph.setUsePercentValues(false)
            behaviourGraph.description.isEnabled = false
            behaviourGraph.isDrawHoleEnabled = true
            behaviourGraph.holeRadius = 50f
            behaviourGraph.transparentCircleRadius = 40f
            behaviourGraph.centerText = "Accelerations & Brakings"
            behaviourGraph.setEntryLabelColor(ContextCompat.getColor(this@JourneyActivity, R.color.black))
            //behaviourGraph.setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
            //behaviourGraph.invalidate()

            val legend = behaviourGraph.legend
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL

            legend.setDrawInside(false)
            legend.isWordWrapEnabled = true
            legend.xEntrySpace = 8f
            legend.yEntrySpace = 4f
            legend.yOffset = 8f

            behaviourGraph.setExtraOffsets(0f, 0f, 0f, 0f)

            behaviourGraph.invalidate()


            val graph2Entries = listOf(
                PieEntry(journey.accTime, "Acceleration time"),
                PieEntry(journey.brakeTime, "Brake time"))


            val graph2Data = PieDataSet(graph2Entries, "Acceleration and Brake time").apply{
                colors = listOf(
                    ContextCompat.getColor(this@JourneyActivity, R.color.bluetooth_blue),
                    ContextCompat.getColor(this@JourneyActivity, R.color.green),
                )

                sliceSpace = 3f
                valueTextSize = 10f
                valueTextColor = R.color.black
                setValueFormatter(DefaultValueFormatter(4))
            }

            timingGraph.data = PieData(graph2Data)

            timingGraph.setUsePercentValues(false)
            timingGraph.description.isEnabled = false
            timingGraph.isDrawHoleEnabled = true
            timingGraph.holeRadius = 50f
            timingGraph.transparentCircleRadius = 40f
            timingGraph.centerText = "Timing"
            timingGraph.setEntryLabelColor(ContextCompat.getColor(this@JourneyActivity, R.color.black))
            //timingGraph.setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
            //behaviourGraph.invalidate()

            val legend2 = timingGraph.legend
            legend2.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend2.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend2.orientation = Legend.LegendOrientation.HORIZONTAL

            legend2.setDrawInside(false)
            legend2.isWordWrapEnabled = true
            legend2.xEntrySpace = 8f
            legend2.yEntrySpace = 4f
            legend2.yOffset = 8f

            timingGraph.setExtraOffsets(0f, 0f, 0f, 0f)

            timingGraph.invalidate()

            closeButton2.setOnClickListener {
                alertDialog2.dismiss()
            }


            alertDialog2.show()
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


    fun service(v: View) {
        val intent = Intent(this, ServiceActivity::class.java)
        startActivity(intent)
    }

    fun home(v: View) {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    fun journey(v: View) {
        val intent = Intent(this, JourneyActivity::class.java)
        startActivity(intent)
    }
}


