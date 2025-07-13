package com.example.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody

class CameraXActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camerax)
        previewView = findViewById(R.id.previewView)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            // Auto-capture after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                captureImage()
            }, 3000)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    if (bitmap != null) {
                        sendToFlask(bitmap)
                    } else {
                        Toast.makeText(this@CameraXActivity, "❗ Image conversion failed", Toast.LENGTH_SHORT).show()
                        finishAfterDelay()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraXActivity, "❗ Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    finishAfterDelay()
                }
            })
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width, image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


    private fun sendToFlask(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageBytes = stream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val json = JSONObject()
        json.put("image_base64", base64Image)

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())


        val request = Request.Builder()
            .url(PrefsHelper.getBaseUrl(this) + "recognize")  // your Flask IP
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CameraXActivity, "❗ API Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    finishAfterDelay()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body?.string()
                runOnUiThread {
                    try {
                        val result = JSONObject(jsonResponse ?: "")
                        val name = result.optString("name", "Unknown")
                        val timestamp = result.optString("timestamp", "")

                        if (name != "Unknown") {
                            Toast.makeText(this@CameraXActivity, "✔️ $name marked at $timestamp", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@CameraXActivity, "❌ Person not in database", Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(this@CameraXActivity, "❗ Invalid response", Toast.LENGTH_SHORT).show()
                    }
                    finishAfterDelay()
                }
            }
        })
    }

    private fun finishAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 5000)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
