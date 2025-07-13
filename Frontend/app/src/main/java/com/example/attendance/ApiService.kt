package com.example.attendance

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class PasscodeData(val passcode: String)
data class LoginResponse(val success: Boolean, val message: String)

interface ApiService {
    @POST("/login")
    fun login(@Body data: PasscodeData): Call<LoginResponse>

}
