package com.example.attendance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    private lateinit var editBaseUrl: EditText
    private lateinit var btnSaveUrl: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editBaseUrl = findViewById(R.id.editBaseUrl)
        btnSaveUrl = findViewById(R.id.btnSaveUrl)

        // Load saved base URL (if any)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentUrl = sharedPreferences.getString("base_url", "")
        editBaseUrl.setText(currentUrl)

        btnSaveUrl.setOnClickListener {
            val enteredUrl = editBaseUrl.text.toString().trim()

            if (enteredUrl.isEmpty()) {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPreferences.edit { putString("base_url", enteredUrl) }
            Toast.makeText(this, "URL saved successfully", Toast.LENGTH_SHORT).show()

            // Go back to MainActivity or close settings
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
