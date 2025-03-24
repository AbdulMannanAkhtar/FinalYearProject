package com.example.mymotor

import android.bluetooth.BluetoothDevice
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//import com.example.bluetooth_v3.Devices


class serviceAdapter (private val scans: MutableList<Scan>, private val responseClick: (Scan) -> Unit):
    RecyclerView.Adapter<serviceAdapter.myViewHolder>() {

    //private var mUser: FirebaseUser? = null
    class myViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val scanID: TextView = itemView.findViewById(R.id.scanId)
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
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
        val formattedTime = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))
        holder.timeStamp.text = "Timestamp: " + formattedTime
        //holder.results.text = scan.obdResponse

        /*
        holder.itemView.setOnClickListener {
            selectDevice(device)
        }
         */

        holder.deleteButton.setOnClickListener {
            deleteScan(scan, position)
        }


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

    fun deleteScan(scan: Scan, position: Int)
    {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance().getReference("users").child(userId).child("scan")

        db.orderByChild("scanID").equalTo(scan.scanID).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(scanSnapshot in snapshot.children)
                {
                    scanSnapshot.ref.removeValue().addOnSuccessListener {
                        scans.removeAt(position)
                        notifyItemRemoved(position)
                        notifyDataSetChanged()

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }


    override fun getItemCount(): Int = scans.size
}