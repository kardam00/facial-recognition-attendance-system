package com.example.attendance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ChangeServerActivity : AppCompatActivity() {

    private lateinit var editUrl: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_server)

        editUrl = findViewById(R.id.editBaseUrl)
        btnSave = findViewById(R.id.btnSaveUrl)

        // Show current URL
        editUrl.setText(PrefsHelper.getBaseUrl(this))

        btnSave.setOnClickListener {
            val url = editUrl.text.toString().trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "Enter valid URL starting with http://", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PrefsHelper.setBaseUrl(this, url)
            Toast.makeText(this, "Server URL updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
