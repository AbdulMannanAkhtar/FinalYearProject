package com.example.mymotor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


const val MESSAGE_KEY = "SIGNUP"

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null



class SignUpActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_signup)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            intent = Intent()

            mAuth = FirebaseAuth.getInstance()

        }


    fun hasDigits(): Boolean {
        val password = findViewById<EditText>(R.id.password)

        val passwordString = password.text.toString()

        for (i in 0 until passwordString.length) {
            if (Character.isDigit(passwordString[i])) {
                return true
            }
        }


        return false
    }


    fun createAccount(v: View)
    {

        //mAuth = FirebaseAuth.getInstance()

        val emailString: String = findViewById<EditText>(R.id.email).text.toString()
        val passwordString: String = findViewById<EditText>(R.id.password).text.toString()
        val confirmPasswordString: String = findViewById<EditText>(R.id.confirmPassword).text.toString()

        if(passwordString.length>= 6 && confirmPasswordString == passwordString && hasDigits())
        {
            mAuth.createUserWithEmailAndPassword(emailString, passwordString).addOnCompleteListener(this)
            {
                task ->
                if(task.isSuccessful)
                {

                    mUser = mAuth.currentUser
                    val userId : String = mUser!!.uid


                    val db: DatabaseReference

                    db = FirebaseDatabase.getInstance().getReference()

                    val user: Users =  Users(emailString)
                    db.child("users").child(userId).setValue(user).addOnSuccessListener{
                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, UserProfileActivity::class.java)
                        startActivity(intent)

                    }
                    
                }

            }

        }
        else
        {
            Toast.makeText(this, "invalid email or password", Toast.LENGTH_SHORT).show()
        }
        
    }
}