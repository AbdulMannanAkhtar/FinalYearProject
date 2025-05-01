package com.example.mymotor

import android.content.Intent
import android.graphics.Color
//import android.icu.util.Calendar
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import java.util.Calendar
import java.util.Objects
import java.util.concurrent.TimeUnit

class BehaviourReportActivity: AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.weekly_report)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = Intent()

        mAuth = FirebaseAuth.getInstance()




        val barChart = findViewById<BarChart>(R.id.barChart)
        val fuelChart = findViewById<BarChart>(R.id.fuelChart)

        weeklyJourneys { journeys: List<Journey> ->
            if (journeys.isEmpty()) {
                return@weeklyJourneys
            }


            val sortedJourneys = journeys.sortedBy {
                it.timestamp
            }

            val xLabels = sortedJourneys.map { journey ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = journey.timestamp
                }
                val day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())!!
                "$day\n ${journey.name}"


            }



            val totalAccels = sortedJourneys.mapIndexed { i, journey ->
                BarEntry(i.toFloat(), journey.totalAccelerations.toFloat())
            }

            val totalBrakes = sortedJourneys.mapIndexed { i, journey ->
                BarEntry(i.toFloat(), journey.totalBrakings.toFloat())
            }

            val aggAccels = sortedJourneys.mapIndexed { i, journey ->
                BarEntry(i.toFloat(), journey.aggressiveAccelerations.toFloat())
            }

            val aggBrakes = sortedJourneys.mapIndexed { i, journey ->
                BarEntry(i.toFloat(), journey.aggressiveBrakings.toFloat())
            }

            val totalAccelsData = BarDataSet(totalAccels, "Total Accelerations").apply {
                color = ContextCompat.getColor(this@BehaviourReportActivity, R.color.bluetooth_blue)
            }
            val totalBrakesData = BarDataSet(totalBrakes, "Total Brakes").apply {
                color = ContextCompat.getColor(this@BehaviourReportActivity, R.color.amber)
            }
            val AggAccelData = BarDataSet(aggAccels, "Aggressive Accelerations").apply {
                color = ContextCompat.getColor(this@BehaviourReportActivity, R.color.green)
            }
            val AggBrakesData = BarDataSet(aggBrakes, "Aggressive Brakes").apply {
                color = ContextCompat.getColor(this@BehaviourReportActivity, R.color.blue_purple)
            }


            val barSpace = 0.05f
            val barWidth = 0.2f
            val groupSpace = 0.3f
            //val barSpace = 0.05f
            val journeyCount = sortedJourneys.size


            val data = BarData(totalAccelsData, totalBrakesData, AggAccelData, AggBrakesData).apply {
                    setBarWidth(barWidth)
                }

            //barChart.setFitBars(false)
            barChart.data = data
            barChart.setFitBars(false)
            barChart.groupBars(0f, groupSpace, barSpace)

            val groupWidth = data.getGroupWidth(groupSpace, barSpace)

            //val firstOffset = offSets.first()
            //val lastOffset = offSets.last()


            barChart.xAxis.axisMinimum = 0f
            barChart.xAxis.axisMaximum =  groupWidth * journeyCount
            barChart.xAxis.granularity = 1f
            barChart.xAxis.isGranularityEnabled = true
            barChart.xAxis.setLabelCount(journeyCount, false)
            barChart.xAxis.setCenterAxisLabels(true)
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels.toTypedArray())
            barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barChart.xAxis.setDrawGridLines(false)
            barChart.xAxis.textColor = Color.BLACK
            barChart.xAxis.setAvoidFirstLastClipping(true)
            //barChart.xAxis.setCenterAxisLabels(true)
            barChart.xAxis.labelRotationAngle = -15f


            //barChart.groupBars(0f, groupSpace, barSpace)
            barChart.description.isEnabled = false


            barChart.axisRight.isEnabled = false


            val maxYValue = listOf(
                sortedJourneys.maxOf{
                    it.totalAccelerations
                },
                sortedJourneys.maxOf{
                    it.totalBrakings
                },
                sortedJourneys.maxOf{
                    it.aggressiveAccelerations
                },
                sortedJourneys.maxOf{
                    it.aggressiveBrakings
                }
            ).maxOrNull()?: 0f



            barChart.axisLeft.axisMinimum =  0f
            barChart.axisLeft.axisMaximum = maxYValue.toFloat()
            barChart.axisLeft.granularity = 1f
            barChart.axisLeft.isGranularityEnabled = true
            //barChart.axisLeft.setLabelCount(maxYValue + 1, true)
            barChart.axisLeft.setDrawGridLines(true)

            barChart.axisLeft.valueFormatter = object: ValueFormatter()
            {
                override fun getFormattedValue(value: Float): String{
                    return value.toInt().toString()
                }
            }

            barChart.description.isEnabled = false

            barChart.legend.apply{
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true



            }

            barChart.setExtraOffsets(16f, 0f, 16f, 32f)
            barChart.animateY(800)
            barChart.invalidate()


            val fuelUse = sortedJourneys.mapIndexed { i, journey ->
                BarEntry(i.toFloat(), journey.usedFuel)

            }

            val fuelUseData = BarDataSet(fuelUse, "Fuel consumption").apply {

                colors = sortedJourneys.map { journey ->
                    if (journey.drivingStyle.equals("aggressive", ignoreCase = true))
                        ContextCompat.getColor(this@BehaviourReportActivity, R.color.red)
                    else
                        ContextCompat.getColor(this@BehaviourReportActivity, R.color.green)
                }.toMutableList()
            }

            val fuelData = BarData(fuelUseData).apply{
                setBarWidth(0.3f)
            }

            fuelChart.data = fuelData

            fuelChart.xAxis.granularity = 1f
            fuelChart.xAxis.isGranularityEnabled = true
            fuelChart.xAxis.labelCount = xLabels.size
            fuelChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels.toTypedArray())
            fuelChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            fuelChart.xAxis.setDrawGridLines(false)
            fuelChart.xAxis.textColor = Color.BLACK
            fuelChart.xAxis.setAvoidFirstLastClipping(true)
            fuelChart.xAxis.labelRotationAngle = -15f

            fuelChart.axisRight.isEnabled = false

            fuelChart.axisLeft.axisMinimum =  0f
            fuelChart.axisLeft.granularity = 1f
            fuelChart.axisLeft.isGranularityEnabled = true
            fuelChart.axisLeft.setDrawGridLines(true)

            fuelChart.axisLeft.valueFormatter = object: ValueFormatter()
            {
                override fun getFormattedValue(value: Float): String{
                    return value.toInt().toString()
                }
            }

            fuelChart.description.isEnabled = false

            fuelChart.legend.apply{
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                isWordWrapEnabled = false
                yOffset = 40f
                xEntrySpace = 5f
                yEntrySpace = 5f
                formToTextSpace = 8f

            }

            fuelChart.legend.isEnabled = true

            val legendKey = listOf(
                LegendEntry(
                    "Aggressive", Legend.LegendForm.SQUARE, 10f, Float.NaN, null,
                    ContextCompat.getColor(this, R.color.red )
                ),
                LegendEntry(
                    "Calm", Legend.LegendForm.SQUARE, 10f, Float.NaN, null,
                    ContextCompat.getColor(this, R.color.green )
                )

            )


            fuelChart.legend.setCustom(legendKey)
            fuelChart.setExtraOffsets(16f, 0f, 16f, 16f)
            fuelChart.animateY(800)
            fuelChart.invalidate()


            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

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
    }

    fun weeklyJourneys(onComplete: (List<Journey>) -> Unit)
    {

        val today = System.currentTimeMillis()
        val week = today - TimeUnit.DAYS.toMillis(7)

        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("journey")

        dbRef.orderByChild("timestamp").startAt(week.toDouble()).endAt(today.toDouble())
            .addListenerForSingleValueEvent(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val journeys = snapshot.children.mapNotNull {
                        it.getValue(Journey::class.java)

                    }
                    onComplete(journeys)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

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



}