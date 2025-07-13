package com.example.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var btnAddPerson: Button
    private lateinit var btnRemovePerson: Button
    private lateinit var btnViewAttendance: Button
    private lateinit var btnChangePasscode: Button
    private lateinit var btnLogout: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        btnAddPerson = findViewById(R.id.btnAddPerson)
        btnRemovePerson = findViewById(R.id.btnRemovePerson)
        btnViewAttendance = findViewById(R.id.btnViewAttendance)
        btnChangePasscode = findViewById(R.id.btnChangePasscode)
        btnLogout = findViewById(R.id.btnLogout)

        btnAddPerson.setOnClickListener {
            startActivity(Intent(this, AddPersonActivity::class.java))
        }

        btnRemovePerson.setOnClickListener {
            startActivity(Intent(this, RemovePersonActivity::class.java))
        }

        btnViewAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceLogActivity::class.java))
        }

        btnChangePasscode.setOnClickListener {
            startActivity(Intent(this, ChangePasscodeActivity::class.java))
        }

        btnLogout.setOnClickListener {
            logout()
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@AdminDashboardActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        })

    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}
