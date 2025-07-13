package com.example.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PrefsHelper {
    private const val PREFS_NAME = "AppPrefs"
    private const val BASE_URL_KEY = "base_url"
    private const val DEFAULT_URL = "http://192.168.29.17:5000/"
    private const val KEY_PASSCODE = "passcode"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(BASE_URL_KEY, DEFAULT_URL) ?: DEFAULT_URL
    }

    @SuppressLint("UseKtx")
    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(BASE_URL_KEY, url).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setPasscode(context: Context, passcode: String) {
        getPrefs(context).edit { putString(KEY_PASSCODE, passcode) }
    }

    fun getPasscode(context: Context): String {
        return getPrefs(context).getString(KEY_PASSCODE, "") ?: ""
    }
}
