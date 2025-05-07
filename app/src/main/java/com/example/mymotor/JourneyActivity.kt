package com.example.mymotor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.health.connect.datatypes.units.Percentage
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
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
import kotlin.math.roundToInt


class JourneyActivity : AppCompatActivity() {


    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private val journeys = mutableListOf<Journey>()
    private lateinit var adapter: journeyAdapter
    //private val scanList=  mutableListOf<Scan>()
    private val interval = 1000L
    private val treshold = 3.0f
    private val noise = 1.0f

    private val accEnter = 0.5f
    private val accExit = 0.1f
    private val brakeEnter = -0.5f
    private val brakeExit = -0.1f

    private var speedListener: ((Float) -> Unit)?     = null
    private var fuelFlowListener: ((Float) -> Unit)?  = null

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

        getJourney.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
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
                    val intent = Intent(this, ScheduleActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }

        }


    }

    fun sampleJourney(): Journey? {
        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser

        if (mUser == null) {
            return null
        }
        val userId: String = mUser!!.uid

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey")

        val cal = Calendar.getInstance().apply{
            timeZone = TimeZone.getDefault()
            set(2025, Calendar.MAY, 4, 10, 0, 0)
        }
        val startTime = cal.timeInMillis
        val endTime =  startTime + 5 * 60 * 1000

        val aggressiveJourney = Journey(
            id = "1", name = "Vist family", to = "Cousins house", from = "Home", timestamp = startTime, usedFuel = 0.200f,
            distance = "5", timeTaken = "0h:5m:00s", endTime  = endTime,
            avgSpeed = 30.0f, totalAccelerations = 10, aggressiveAccelerations = 5, totalBrakings = 9, aggressiveBrakings = 5,
            drivingStyle = "aggressive", accTime = 8.2f, brakeTime = 7.8f
        )

        val key = db.push().key ?: return null
        aggressiveJourney.id = key
        db.child(key).setValue(aggressiveJourney)

        return aggressiveJourney


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

        //val oldListener = connectedThread?.messageRecieved



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

                }


            }

        }
        connectedThread?.sendCommand("012F")
    }

    fun convertPercentageToLiters(percentage: Float, tankCapacity: Float = 70.00f): Float
    {
        return (percentage / 100f) * tankCapacity
    }

    fun getInstantFuelFlow(callback: (fuelLPerH: Float) -> Unit) {

        // 1) grab your thread and the raw socket
        val thread = HomeActivity.Companion.connectedThread
        val socket = HomeActivity.BluetoothSocketHolder.socket


        if (thread == null || socket == null || !socket.isConnected) {
            callback(0f)
            return
        }


        val oldListener = thread.messageRecieved
        thread.messageRecieved = { msg ->
            runOnUiThread {
                val parts = msg.trim().split(" ")
                if (parts.size >= 4 && parts[0] == "41" && parts[1] == "10")
                {

                    val A   = parts[2].toInt(16)
                    val B   = parts[3].toInt(16)
                    val maf = (A * 256 + B) / 100f
                    val fuelGPerS = maf / 14.7f
                    val fuelLPerS  = fuelGPerS / 745f
                    val fuelLPerH  = fuelLPerS * 3600f

                    callback(fuelLPerH)

                    thread.messageRecieved = oldListener
                } else {

                    oldListener?.invoke(msg)
                }
            }
        }


        thread.sendCommand("0110")
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

        val minSpeed = 5f

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        behaviourRunnable = object : Runnable {
            override fun run() {
                getSpeed { currentSpeedKmh ->
                    Log.d("DriverBehavior", "Current speed: $currentSpeedKmh km/h")
                    val currentTime = System.currentTimeMillis()


                    if(prevSpeed != null && prevTime != null)
                    {

                        val delta = (currentTime - prevTime!!) / 1000f

                        if (delta > 0f) {
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
                                journey.accTime += delta

                            }

                            else if (acceleration < -noise)
                            {
                                Log.d("DriverBehavior", "  ▶ counted BRK burst")
                                currentState = -1
                                journey.brakeTime += delta

                            }

                            else
                            {
                                currentState = 0
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
                        getInstantFuelFlow {
                            fuelLPerH ->

                            journey.usedFuel += fuelLPerH * (delta / 3600f)
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



                    mUser = FirebaseAuth.getInstance().currentUser
                    val userId: String = mUser!!.uid

                    val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()
                    var journeyReference: DatabaseReference

                    val journeyNameString: String = journeyName.text.toString()
                    val StringTo: String = to.text.toString()
                    val fromString: String = from.text.toString()

                    val journeyID = db.push().key!!
                    val journeyName = journeyNameString
                    val timestamp = System.currentTimeMillis()

                    val journey = Journey(
                        journeyID, journeyName, StringTo, fromString, timestamp,
                        lat, lon, startFuel = 0f)

                    driverBehaviour(journey)

                    journey(journey)

                    alertDialog.dismiss()


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
/*
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


 */

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

            saveToFirebase(journey)




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

        val used = journey.usedFuel
        val format = String.format("%.4f", used)

        journeyName.text = journey.name
        travelled.text = journey.from + " to " + journey.to
        fuel.text = "You consumed: " + format + " liters of fuel"
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


