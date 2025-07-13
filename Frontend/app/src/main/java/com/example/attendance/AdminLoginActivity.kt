package com.example.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import androidx.activity.OnBackPressedCallback

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var editPasscode: EditText
    private lateinit var btnAdminLogin: Button
    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@AdminLoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)


        editPasscode = findViewById(R.id.editPasscode)
        btnAdminLogin = findViewById(R.id.btnAdminLogin)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, ChangeServerActivity::class.java))
        }

        val togglePassVisibility = findViewById<ImageButton>(R.id.togglePassVisibility)
        var isVisible = false

        togglePassVisibility.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                editPasscode.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePassVisibility.setImageResource(R.drawable.ic_eye_off)
            } else {
                editPasscode.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePassVisibility.setImageResource(R.drawable.ic_eye)
            }
            editPasscode.setSelection(editPasscode.text.length) // move cursor to end
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(PrefsHelper.getBaseUrl(this)) // ✅ Replace with your backend IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)

        btnAdminLogin.setOnClickListener {
            val passcode = editPasscode.text.toString().trim()

            if (passcode.isEmpty()) {
                Toast.makeText(this, "Please enter passcode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.login(PasscodeData(passcode)).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AdminLoginActivity, "Login successful", Toast.LENGTH_SHORT).show()

                        PrefsHelper.setPasscode(this@AdminLoginActivity, passcode)

                        val intent = Intent(this@AdminLoginActivity, AdminDashboardActivity::class.java)
                        startActivity(intent)

                        finish()
                    } else {
                        Toast.makeText(this@AdminLoginActivity, "Invalid passcode", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@AdminLoginActivity, "API error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
