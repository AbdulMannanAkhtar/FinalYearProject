package com.example.mymotor

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//import com.example.bluetooth_v3.Devices


class serviceAdapter (val scans: MutableList<Scan>, private val responseClick: (Scan) -> Unit):
    RecyclerView.Adapter<serviceAdapter.myViewHolder>() {

    //private var mUser: FirebaseUser? = null
    class myViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val scanID: TextView = itemView.findViewById(R.id.scanId)
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
        val codes: TextView = itemView.findViewById(R.id.codes)
        val tap: TextView = itemView.findViewById(R.id.tap)
        //val deleteButton: Button = itemView.findViewById(R.id.Delete)
       // val results: TextView = itemView.findViewById(R.id.results)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rowlayout2, parent, false)

        return myViewHolder(view)

    }


    override fun onBindViewHolder(holder: myViewHolder, position: Int) {

        val scan = scans[position]
// mUser = FirebaseAuth.getInstance().currentUser
  //      val userId: String = mUser!!.uid

        holder.scanID.text = "ID:" + scan.scanID
        //val formattedTime = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))
        holder.timeStamp.text = getRelativeTime(scan.timestamp)
        //holder.results.text = scan.obdResponse

        val codes = summarizeSeverity(scan, loadDescriptions(holder.itemView.context))
        holder.codes.text = "Severities: $codes"




        /*
        holder.itemView.setOnClickListener {
            selectDevice(device)
        }
         */

/*
        holder.deleteButton.setOnClickListener {
            deleteScan(scan)
        }

 */


        holder.itemView.setOnClickListener{
            responseClick(scan)
        }




    }

    /*
    fun update(updateList: List<Scan>)
    {
        scans.clear()
        scans.addAll(updateList)
        notifyDataSetChanged()
    }

     */

    fun deleteScan(scan: Scan)
    {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("scan")

        db.orderByChild("scanID").equalTo(scan.scanID).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(scanSnapshot in snapshot.children)
                {
                    scanSnapshot.ref.removeValue().addOnSuccessListener {
                        val index = scans.indexOfFirst{
                            it.scanID == scan.scanID
                        }

                        if(index != -1)
                        {
                            scans.removeAt(index)
                            notifyItemRemoved(index)
                            notifyDataSetChanged()
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

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

    fun parseObdCodes(rawResponse: String): List<String> {

        val cleaned = rawResponse.replace(">", "").trim()
        return cleaned.split(" ").filter { it.matches(Regex("^[A-Z0-9]+$")) }
    }

    fun summarizeSeverity(scan: Scan, descriptions: Map<String,ObdDescriptions>): String {
        val codes = parseObdCodes(scan.obdResponse)

        val filteredCodes = codes.filterNot{
            it == "03" || it == "43"
        }

        val frequency = mutableMapOf<String, Int>()

        for(c in filteredCodes)
        {
            val desc = descriptions[c]

            val severity = desc?.severity ?: "Unknown"

            frequency[severity] = (frequency[severity] ?: 0) + 1


        }

        return if (frequency.isNotEmpty())
        {
            frequency.entries.joinToString { "${it.value} ${it.key}"}
        } else {
            "No codes"
        }


    }

    fun getRelativeTime(timestamp:Long): String
    {
        return DateUtils.getRelativeTimeSpanString(
            timestamp,System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }


    override fun getItemCount(): Int = scans.size
}