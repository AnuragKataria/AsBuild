package com.rbt.survey

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rbt.survey.location.LocationService
import com.rbt.survey.data.local.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = UserPreferences(this)

        CoroutineScope(Dispatchers.IO).launch {
            val token = prefs.refreshToken.first()

            // ✅ If already logged in → start service
            if (!token.isNullOrEmpty()) {
                val intent = Intent(this@MyApplication, LocationService::class.java)
                ContextCompat.startForegroundService(this@MyApplication, intent)
            }
        }
    }
}