package com.example.mymotor
import android.content.Intent
import android.net.Uri
//import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.FileInputStream

const val MESSAGE_KEY5 = "PROFILE_CAR"

private lateinit var mAuth: FirebaseAuth
private var mUser: FirebaseUser? = null
private var  imageUri: Uri? = null
private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

class CarProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_carprofile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = Intent()

        mAuth = FirebaseAuth.getInstance()

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent())
        { uri ->

            if (uri != null) {
                imageUri = uri

                findViewById<ImageButton>(R.id.carImageBtn).setImageURI(imageUri)

            }

        }
    }

    fun uploadImg(v: View) {

        imagePickerLauncher.launch("image/*")

    }

    fun createProfile(v: View) {
        mUser = FirebaseAuth.getInstance().currentUser
        val userId: String = mUser!!.uid

        val makeString: String = findViewById<EditText>(R.id.carMake).text.toString()
        val modelString: String = findViewById<EditText>(R.id.carModel).text.toString()
        val yearString: String = findViewById<EditText>(R.id.carYear).text.toString()
        val yearInt = yearString.toInt()

            val db: DatabaseReference = FirebaseDatabase.getInstance().getReference()
            val car = Car(makeString, modelString, yearInt)

            db.child("users").child(userId).child("car").setValue(car).addOnSuccessListener {
                Toast.makeText(this, "Car added", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)


            }
    }
}



/*
        if(imageUri != null)
        {

           //val storage = FirebaseStorage.getInstance().reference.child("carImages/${userId}.png")
            val storage = FirebaseStorage.getInstance().getReference("carImages/$userId.png")



            storage.putFile(imageUri!!).addOnSuccessListener {

                   Log.d("CarProfileActivity", "Image URI: ${imageUri.toString()}")
                   Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show()


                   val intent = Intent(this, HomeActivity::class.java)
                   startActivity(intent)
               }



        }
        else {

            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

*/
