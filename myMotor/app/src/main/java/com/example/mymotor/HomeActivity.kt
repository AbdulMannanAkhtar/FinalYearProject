package com.example.mymotor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null
private var  imageUri: Uri? = null

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = Intent()

        mAuth = FirebaseAuth.getInstance()

        val db: DatabaseReference
        val userId = mAuth.currentUser?.uid

        db = FirebaseDatabase.getInstance().getReference("users/$userId/car")

        val carDetails = findViewById<TextView>(R.id.carDetails)

        db.addValueEventListener(object: ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                val make = snapshot.child("make").getValue(String::class.java)
                val model = snapshot.child("model").getValue(String::class.java)
                val year = snapshot.child("year").getValue(Int::class.java)

                val car = "$make\n$model\n$year"
                carDetails.text = car
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })



    }





    }