package com.example.attendance

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class RecognitionActivity : AppCompatActivity() {

    private lateinit var btnEntry: Button
    private lateinit var btnExit: Button
    private lateinit var textResult: TextView

    private var currentMode = "entry"
    private val cameraRequestCode = 100

    private val startCameraForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val photo = result.data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    sendImageToServer(photo, currentMode)
                } else {
                    textResult.setText(R.string.failed_capture)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognition)

        btnEntry = findViewById(R.id.btnEntry)
        btnExit = findViewById(R.id.btnExit)
        textResult = findViewById(R.id.textResult)

        btnEntry.setOnClickListener {
            currentMode = "entry"
            checkCameraPermissionAndOpen()
        }

        btnExit.setOnClickListener {
            currentMode = "exit"
            checkCameraPermissionAndOpen()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                cameraRequestCode
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startCameraForResult.launch(intent)
    }

    private fun sendImageToServer(bitmap: Bitmap, mode: String) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageBytes = stream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val json = mapOf("image_base64" to base64Image, "mode" to mode)
        val body = JSONObject(json).toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("${PrefsHelper.getBaseUrl(this)}/recognize")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    textResult.text = getString(R.string.api_failed_message, e.message)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val result = JSONObject(response.body?.string() ?: "{}")
                val name = result.optString("name", "Unknown")
                val time = result.optString("timestamp", "")
                val status = result.optString("status", "")

                val message = when {
                    status == "duplicate" && currentMode == "entry" -> getString(R.string.entry_already_marked, name)
                    status == "duplicate" && currentMode == "exit" -> getString(R.string.exit_already_marked, name)
                    status == "ok" && name != "Unknown" -> getString(R.string.attendance_marked, currentMode, name, time)
                    else -> getString(R.string.face_not_recognized)
                }

                runOnUiThread {
                    textResult.text = message
                }

                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 5000)
            }
        })
    }
}
