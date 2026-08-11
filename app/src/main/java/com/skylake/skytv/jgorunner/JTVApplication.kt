package com.skylake.skytv.jgorunner

import android.app.Application
import android.util.Log

class JTVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupUncaughtExceptionHandler()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("JTVApplication", "Uncaught exception intercepted in thread ${thread.name}: ${throwable.message}", throwable)
                com.skylake.skytv.jgorunner.utils.LogCollector.logError("Uncaught exception intercepted: ${throwable.message}", throwable)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
