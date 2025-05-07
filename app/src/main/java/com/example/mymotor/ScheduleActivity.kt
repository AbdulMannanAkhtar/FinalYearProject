package com.example.mymotor

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

class ScheduleActivity : AppCompatActivity() {


    private lateinit var mAuth: FirebaseAuth
    private var mUser: FirebaseUser? = null
    private val schedules = mutableListOf<Schedule>()
    private lateinit var adapter: scheduleAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule)

        mAuth = FirebaseAuth.getInstance()
        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = scheduleAdapter(schedules)
        {
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        intent = Intent()

        //sampleJourney()


        val getSchedule: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users/$userId/reminders")

        getSchedule.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //OBDScans.clear()
                val tempList = mutableListOf<Schedule>()

                for (scanSnapshot in snapshot.children) {

                    val scheduleClass = scanSnapshot.getValue(Schedule::class.java)

                    if (scheduleClass != null) {

                        tempList.add(scheduleClass)
                    }


                }

                schedules.clear()
                schedules.addAll(tempList)
                adapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
            }
        })

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.scheduleButton

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

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
                    if (this !is ScheduleActivity) {
                        val intent = Intent(this, ScheduleActivity::class.java)
                        startActivity(intent)
                    }
                    true
                }

                else -> false
            }

        }


    }

    fun notification(schedule: Schedule, reminderIn: Int)
    {
        val triggerAt  = schedule.date - reminderIn * 24L * 60 * 60 * 1000

        if(triggerAt <= System.currentTimeMillis())
        {
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if(!alarmManager.canScheduleExactAlarms())
            {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply{
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)

                return
            }
        }

        val intent = Intent(this, ExpiryAlarmReciever::class.java).putExtra("message",
            "$reminderIn days until ${schedule.name}")

        val pendingIntent = PendingIntent.getBroadcast(this, schedule.id.hashCode() + reminderIn, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }


    fun newReminder(v: View)
    {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialog: View = inflater.inflate(R.layout.add_reminder, null)

        val build: AlertDialog.Builder = AlertDialog.Builder(this)
        build.setView(dialog)

        val alertDialog = build.create()
        alertDialog.show()

        val newReminder: EditText = dialog.findViewById(R.id.newReminder)
        val datePicker: DatePicker = dialog.findViewById(R.id.datePicker)

        val addReminder: Button = dialog.findViewById(R.id.addReminder)
        val closeButton: Button = dialog.findViewById(R.id.close6)

        addReminder.setOnClickListener {
            val name: String = newReminder.text.toString()

            val cal = Calendar.getInstance().apply{

                set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 9, 0, 0)
            }
            val date = cal.timeInMillis

            mUser = FirebaseAuth.getInstance().currentUser
            val userId: String = mUser!!.uid


            val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()


            val scheduleID  = db.push().key ?: return@setOnClickListener

            val schedule = Schedule(scheduleID, name, date, reminder1 = 30, reminder2 = 7)

            var scheduleReference: DatabaseReference

            FirebaseDatabase.getInstance().getReference("users").child(userId).child("reminders").child(schedule.id)
                .setValue(schedule).addOnSuccessListener {
                    Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()
                }

            notification(schedule, 30)
            notification(schedule, 7)




        }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
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


}