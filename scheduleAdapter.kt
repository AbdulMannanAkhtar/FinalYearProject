package com.example.mymotor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class scheduleAdapter (private val schedules: MutableList<Schedule>, private val responseClick: (Schedule) -> Unit):
    RecyclerView.Adapter<scheduleAdapter.myViewHolder>() {

        private val dateFormatter = SimpleDateFormat("dd:MM:yyyy", Locale.getDefault())

    //private var mUser: FirebaseUser? = null
    class myViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val reminderName: TextView = itemView.findViewById(R.id.reminderName)
        val reminderDate: TextView = itemView.findViewById(R.id.reminderDate)



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rowlayout5, parent, false)

        return myViewHolder(view)

    }


    override fun onBindViewHolder(holder: myViewHolder, position: Int) {

        val schedule = schedules[position]
// mUser = FirebaseAuth.getInstance().currentUser
        //      val userId: String = mUser!!.uid

        holder.reminderName.text =  schedule.name
        holder.reminderDate.text = dateFormatter.format(Date(schedule.date))

        holder.itemView.setOnClickListener{
            responseClick(schedule)
        }




    }


    override fun getItemCount(): Int = schedules.size
}