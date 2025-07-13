package com.example.attendance

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnMark = findViewById<Button>(R.id.btnMarkAttendance)
        val btnAdmin = findViewById<Button>(R.id.btnAdminLogin)

        btnMark.setOnClickListener {
            // TODO: start RecognitionActivity
            val intent = Intent(this, RecognitionActivity::class.java)
            startActivity(intent)
        }

        btnAdmin.setOnClickListener {
            // TODO: start LoginActivity
            val intent = Intent(this, AdminLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)


        }
    }
}
