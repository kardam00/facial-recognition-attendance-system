package com.example.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class AddPersonActivity : AppCompatActivity() {

    private lateinit var editEmpName: EditText
    private lateinit var editEmpId: EditText
    private lateinit var btnCaptureFaces: Button
    private lateinit var textStatus: TextView

    private val capturedImages = mutableListOf<Bitmap>()
    private val cameraRequest = 101
    private val cameraPermissionCode = 1001
    private var captureCount = 0
    private lateinit var name: String
    private lateinit var empId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_person)

        editEmpName = findViewById(R.id.editEmpName)
        editEmpId = findViewById(R.id.editEmpId)
        btnCaptureFaces = findViewById(R.id.btnCaptureFaces)
        textStatus = findViewById(R.id.textStatus)

        btnCaptureFaces.setOnClickListener {
            name = editEmpName.text.toString().trim()
            empId = editEmpId.text.toString().trim()

            if (name.isEmpty() || empId.isEmpty()) {
                Toast.makeText(this, R.string.enter_name_id, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject().apply {
                put("emp_id", empId)
                put("passcode", PrefsHelper.getPasscode(this@AddPersonActivity))
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(PrefsHelper.getBaseUrl(this) + "check_person_exists")
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddPersonActivity,
                            getString(R.string.error_message, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val bodyString = response.body?.string()
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            textStatus.text = "Unauthorized. Please check your passcode."
                            return@runOnUiThread
                        }

                        try {
                            val result = JSONObject(bodyString ?: "{}")
                            val exists = result.optBoolean("exists", false)

                            if (exists) {
                                textStatus.setText(R.string.person_exists)
                                editEmpName.text.clear()
                                editEmpId.text.clear()
                            } else {
                                captureCount = 0
                                capturedImages.clear()
                                checkAndRequestCameraPermission()
                            }
                        } catch (e: Exception) {
                            textStatus.text = "Unexpected server response."
                            Log.e("API_DEBUG", "JSON parsing failed", e)
                        }
                    }
                }
            })
        }
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        } else {
            captureImage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage()
            } else {
                Toast.makeText(this, "Camera permission is required to capture faces.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureImage() {
        if (captureCount < 5) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, cameraRequest)
        } else {
            sendImagesToBackend()
        }
    }

    @Deprecated("Deprecated API usage", ReplaceWith("registerForActivityResult()"))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == cameraRequest && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val image = data?.extras?.getParcelable("data") as? Bitmap
            image?.let {
                capturedImages.add(it)
                captureCount++
                Toast.makeText(
                    this,
                    getString(R.string.capture_progress, captureCount),
                    Toast.LENGTH_SHORT
                ).show()
                captureImage()
            }
        }
    }

    private fun sendImagesToBackend() {
        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        multipartBuilder.addFormDataPart("name", "$name [$empId]")
        multipartBuilder.addFormDataPart("passcode", PrefsHelper.getPasscode(this@AddPersonActivity))

        capturedImages.forEachIndexed { index, bitmap ->
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()

            val reqFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            multipartBuilder.addFormDataPart("images", "img$index.jpg", reqFile)
        }

        val request = Request.Builder()
            .url(PrefsHelper.getBaseUrl(this@AddPersonActivity) + "add_person")
            .post(multipartBuilder.build())
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    textStatus.text = getString(R.string.failed_to_add, e.message)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                val message = try {
                    JSONObject(bodyString ?: "{}").optString("message", getString(R.string.no_message))
                } catch (e: Exception) {
                    Log.e("API_DEBUG", "Error parsing add_person response", e)
                    getString(R.string.no_message)
                }

                runOnUiThread {
                    textStatus.text = message

                    if (message.contains("success", true)) {
                        android.os.Handler(mainLooper).postDelayed({
                            val intent = Intent(this@AddPersonActivity, AdminDashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }, 2000)
                    } else {
                        editEmpName.text.clear()
                        editEmpId.text.clear()
                    }
                }
            }
        })
    }
}
