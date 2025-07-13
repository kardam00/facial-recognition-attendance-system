package com.example.attendance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import android.content.Intent
import okhttp3.RequestBody.Companion.toRequestBody



class RemovePersonActivity : AppCompatActivity() {

    private lateinit var editRemoveName: EditText
    private lateinit var editRemoveId: EditText
    private lateinit var btnRemovePerson: Button
    private lateinit var textRemoveStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_person)

        editRemoveName = findViewById(R.id.editRemoveName)
        editRemoveId = findViewById(R.id.editRemoveId)
        btnRemovePerson = findViewById(R.id.btnRemovePerson)
        textRemoveStatus = findViewById(R.id.textRemoveStatus)

        btnRemovePerson.setOnClickListener {
            val name = editRemoveName.text.toString().trim()
            val id = editRemoveId.text.toString().trim()

            if (name.isEmpty() || id.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_name_id), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject()
            json.put("name", "$name [$id]")
            json.put("passcode", PrefsHelper.getPasscode(this@RemovePersonActivity))

            val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(PrefsHelper.getBaseUrl(this) + "remove_person")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        textRemoveStatus.text = getString(R.string.remove_failed, e.message)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = JSONObject(response.body?.string() ?: "{}")
                    val msg = result.optString("message", result.optString("error", "Unknown error"))
                    runOnUiThread {
                        textRemoveStatus.text = getString(R.string.remove_message, msg)

                        if (msg.contains("success", true)) {
                            android.os.Handler(mainLooper).postDelayed({
                                val intent = Intent(this@RemovePersonActivity, AdminDashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            }, 2000)
                        } else {
                            editRemoveName.text.clear()
                            editRemoveId.text.clear()
                        }
                    }

                }
            })
        }
    }
}
