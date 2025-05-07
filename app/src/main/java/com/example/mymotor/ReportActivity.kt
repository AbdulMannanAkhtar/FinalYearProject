package com.example.mymotor

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportActivity: AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = Intent()

        mAuth = FirebaseAuth.getInstance()

        val barChart = findViewById<BarChart>(R.id.barChart)
        sixMonthsGraph(this, barChart)

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
                    val intent = Intent(this, ScheduleActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }

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

    fun monthLabels(): List<String>
    {
        val months = mutableListOf<String>()

        val sdf = SimpleDateFormat("MM:yyyy", Locale.getDefault())

        val calendar = Calendar.getInstance()

        for(i in 5 downTo 0)
        {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            months.add(sdf.format(cal.time))
        }

        return months

    }
    fun parseObdCodes(rawResponse: String): List<String> {

        val cleaned = rawResponse.replace(">", "").trim()
        return cleaned.split(" ").filter { it.matches(Regex("^[A-Z0-9]+$")) }
    }

    fun sixMonthsGraph(context: Context, barChart: BarChart)
    {
        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val severity = loadDescriptions(this)

        val severityCategory = listOf("No issues", "Moderate", "Urgent")

        val months = monthLabels()

        val graphData = mutableMapOf<String, MutableMap<String, Int>>()

        for(m in months)
        {

            val countSeverity =  mutableMapOf<String, Int>()
            severityCategory.forEach{
                countSeverity[it] = 0
            }
            graphData[m] = countSeverity
        }

        val sixMonthsCalender = Calendar.getInstance()
        sixMonthsCalender.add(Calendar.MONTH, -6)

        val sixMonthsTime = sixMonthsCalender.timeInMillis

        val responseCodes = FirebaseDatabase.getInstance().getReference("users/$userId/scan")

        responseCodes.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               for(responseSnapshot in snapshot.children)
               {
                   val code = responseSnapshot.child("obdResponse").getValue(String::class.java) ?: continue
                   //val timestamp = responseSnapshot.child("timestamp").value?.toString() ?: continue

                   val timestampObj = responseSnapshot.child("timestamp").value
                   val timestamp = when (timestampObj) {
                       is Long -> timestampObj
                       is String -> timestampObj.toLongOrNull() ?: continue
                       else -> continue
                   }

                   if(timestamp < sixMonthsTime) continue


                   val sdf = SimpleDateFormat("MM:yyyy", Locale.getDefault())
                   val monthLabel = sdf.format(Date(timestamp))


                   val codeList = parseObdCodes(code)

                   for (code in codeList)
                   {
                       if(code == "03" || code == "43")
                       {
                           continue
                       }
                       val description = severity[code] ?: continue

                       val rawSeverity = description.severity ?: "Unknown"

                       val finalSeverity = when (description?.severity) {
                           "No issues" -> "No issues"
                           else -> rawSeverity
                       }

                       if (finalSeverity in listOf("No issues", "Moderate", "Urgent")) {
                           val monthMap = graphData[monthLabel] ?: continue
                           monthMap[finalSeverity] = (monthMap[finalSeverity] ?: 0) + 1
                       }


                   }



               }

                val noSeverity = mutableListOf<BarEntry>()
                val moderateSeverity = mutableListOf<BarEntry>()
                val urgentSeverity = mutableListOf<BarEntry>()

                Log.d("GraphDebug", "Final Aggregation: $graphData")

                for((index, m) in months.withIndex())
                {
                    val counts = graphData[m]!!
                    noSeverity.add(BarEntry(index.toFloat(), counts["No issues"]?.toFloat() ?: 0f))
                    moderateSeverity.add(BarEntry(index.toFloat(), counts["Moderate"]?.toFloat() ?: 0f))
                    urgentSeverity.add(BarEntry(index.toFloat(), counts["Urgent"]?.toFloat() ?: 0f))
                }

                val noSeverityDataSet = BarDataSet(noSeverity, "No issues").apply{
                    color = Color.GREEN
                }


                val moderateDataSet = BarDataSet(moderateSeverity, "Moderate").apply{
                    color = Color.YELLOW
                }

                val urgentDataSet = BarDataSet(urgentSeverity, "Urgent").apply {
                    color = Color.RED
                }


                val data = BarData(noSeverityDataSet, moderateDataSet, urgentDataSet)
                val barWidth = 0.2f
                barChart.legend.isEnabled = true

                val groupCount = months.size
                val groupSpace = 0.3f
                val barSpace = 0.05f
                data.barWidth = barWidth
                barChart.data = data

                //noSeverityDataSet.barBorderWidth = barWidth

               // val groupWith = data.getGroupWidth(groupSpace, barSpace)

                val startOffSet = 0.1f

                barChart.axisLeft.axisMinimum = 0f
                barChart.axisRight.axisMinimum = 0f
                barChart.xAxis.axisMinimum = 0f
                barChart.xAxis.axisMaximum = 0f + data.getGroupWidth(groupSpace, barSpace) * groupCount
                barChart.xAxis.granularity = 1f
                barChart.xAxis.isGranularityEnabled = true
                barChart.xAxis.setDrawLabels(true)
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
                barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                barChart.xAxis.textColor = Color.BLACK

                barChart.axisRight.isEnabled = false

                barChart.axisLeft.axisMinimum =  0f
                barChart.axisLeft.granularity = 1f
                barChart.axisLeft.isGranularityEnabled = true
                barChart.axisLeft.setDrawGridLines(true)

                barChart.axisLeft.valueFormatter = object: ValueFormatter()
                {
                    override fun getFormattedValue(value: Float): String{
                        return value.toInt().toString()
                    }
                }



                barChart.groupBars(startOffSet, groupSpace, barSpace)
                barChart.invalidate()



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