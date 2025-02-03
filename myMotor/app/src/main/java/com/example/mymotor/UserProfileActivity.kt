
package com.example.mymotor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

const val MESSAGE_KEY4 = "PROFILE"

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null

class UserProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_userprofile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val genderSpinner: Spinner = findViewById<Spinner>(R.id.gender)
        ArrayAdapter.createFromResource(this, R.array.gender_options, android.R.layout.simple_spinner_item)
            .also{adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                genderSpinner.adapter = adapter
            }


        intent = Intent()


    }
    fun next(v: View) {

        mUser = FirebaseAuth.getInstance().currentUser

        val userId : String = mUser!!.uid

        val nameString: String = findViewById<EditText>(R.id.fullName).text.toString()
        val ageString: String = findViewById<EditText>(R.id.age).text.toString()
        val ageInt = ageString.toInt()
        val genderSpinner: Spinner = findViewById<Spinner>(R.id.gender)
        val genderString = genderSpinner.selectedItem.toString()
        val addressString: String = findViewById<EditText>(R.id.address).text.toString()


        if(genderString == "Gender")
        {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
            return
        }

        val db: DatabaseReference

        db = FirebaseDatabase.getInstance().getReference()

        val profile: Profile = Profile(nameString, ageInt, genderString, addressString)
        db.child("users").child(userId).child("profile").setValue(profile).addOnSuccessListener {
            Toast.makeText(this, "Profile added", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, CarProfileActivity::class.java)
            startActivity(intent)

        }


    }
}

