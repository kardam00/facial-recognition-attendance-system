package com.example.attendance

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.content.Intent


class ChangePasscodeActivity : AppCompatActivity() {

    private lateinit var editOldPass: EditText
    private lateinit var editNewPass: EditText
    private lateinit var btnSubmit: Button
    private lateinit var statusText: TextView

    private fun toggleVisibility(editText: EditText) {
        val inputType = editText.inputType
        if (inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text.length)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_passcode)

        editOldPass = findViewById(R.id.editOldPasscode)
        editNewPass = findViewById(R.id.editNewPasscode)
        btnSubmit = findViewById(R.id.btnSubmitPasscode)
        statusText = findViewById(R.id.textStatus)

        val baseUrl = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("base_url", PrefsHelper.getBaseUrl(this)) ?: ""

        val toggleOldPass = findViewById<ImageButton>(R.id.toggleOldPass)
        val toggleNewPass = findViewById<ImageButton>(R.id.toggleNewPass)

        var isOldVisible = false
        var isNewVisible = false

        toggleOldPass.setOnClickListener {
            isOldVisible = !isOldVisible
            toggleVisibility(editOldPass)
            toggleOldPass.setImageResource(if (isOldVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        toggleNewPass.setOnClickListener {
            isNewVisible = !isNewVisible
            toggleVisibility(editNewPass)
            toggleNewPass.setImageResource(if (isNewVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        btnSubmit.setOnClickListener {
            val oldPass = editOldPass.text.toString().trim()
            val newPass = editNewPass.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Both fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject()
            json.put("old_passcode", oldPass)
            json.put("new_passcode", newPass)

            val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/change_passcode")
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        statusText.text = getString(R.string.failed_message, e.message)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = JSONObject(response.body?.string() ?: "{}")
                    val message = result.optString("message", getString(R.string.default_no_message))
                    runOnUiThread {
                        if (response.isSuccessful) {
                            statusText.text = getString(R.string.success_message, message)
                            android.os.Handler(mainLooper).postDelayed({
                                val intent = Intent(this@ChangePasscodeActivity, AdminDashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            }, 2000)
                        } else {
                            statusText.text = getString(R.string.error_message, message)
                        }


                    }
                }
            })
        }
    }
}
