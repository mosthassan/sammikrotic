package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class SamMikrotikApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Setup global safety uncaught exception logging so crashes are logged cleanly
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SamMikrotikApplication", "FATAL EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            Log.i("SamMikrotikApplication", "FirebaseApp initialized successfully")
        } catch (e: Throwable) {
            Log.e("SamMikrotikApplication", "FirebaseApp init fallback: ${e.message}", e)
        }
    }
}
