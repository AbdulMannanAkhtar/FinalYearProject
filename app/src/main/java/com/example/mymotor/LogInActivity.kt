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
import com.example.mymotor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

const val MESSAGE_KEY3 = "LOGIN"

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null



class LogInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = Intent()

        mAuth = FirebaseAuth.getInstance()

    }


    fun logIn(v: View) {

        //mAuth = FirebaseAuth.getInstance()

        val emailString: String = findViewById<EditText>(R.id.logInEmail).text.toString()
        val passwordString: String = findViewById<EditText>(R.id.logInPassword).text.toString()

        if (passwordString.isNotEmpty() && emailString.isNotEmpty()) {
            mAuth.signInWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener(this)
                { task ->
                    if (task.isSuccessful) {

                        mUser = mAuth.currentUser
                        Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)


                    }

                }

        } else {
            Toast.makeText(this, "invalid email or password", Toast.LENGTH_SHORT).show()
        }


    }
}