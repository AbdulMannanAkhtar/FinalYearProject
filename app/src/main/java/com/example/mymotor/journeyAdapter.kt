
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mymotor.Journey
import com.example.mymotor.R
import com.example.mymotor.Scan
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


class journeyAdapter (private val journeys: MutableList<Journey>, private val responseClick: (Journey) -> Unit):
    RecyclerView.Adapter<journeyAdapter.myViewHolder>() {

    //private var mUser: FirebaseUser? = null
    class myViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        val journeyName: TextView = itemView.findViewById(R.id.journeyName)
        val toLoc: TextView = itemView.findViewById(R.id.to)
        val fromLoc: TextView = itemView.findViewById(R.id.from)
        val timeTake: TextView = itemView.findViewById(R.id.timeTaken)
        val distanceTravelled: TextView = itemView.findViewById(R.id.distanceTravelled)
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rowlayout3, parent, false)

        return myViewHolder(view)

    }


    override fun onBindViewHolder(holder: myViewHolder, position: Int) {

        val journey = journeys[position]
// mUser = FirebaseAuth.getInstance().currentUser
        //      val userId: String = mUser!!.uid

        holder.journeyName.text =  journey.name
        val formattedTime = "Date: " + SimpleDateFormat("dd:MM:yyyy", Locale.getDefault()).format(Date(journey.timestamp))
        holder.timeStamp.text = formattedTime
        holder.toLoc.text = "To: " + journey.to
        holder.fromLoc.text = "From: " + journey.from
        holder.timeTake.text = journey.timeTaken
        holder.distanceTravelled.text = journey.distance + "Kms"

        /*
        holder.itemView.setOnClickListener {
            selectDevice(device)
        }
         */

        /*
        holder.deleteButton.setOnClickListener {
            deleteScan(scan, position)
        }#

         */


        holder.itemView.setOnClickListener{
            responseClick(journey)
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

    /*
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

     */



    override fun getItemCount(): Int = journeys.size
}