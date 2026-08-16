package com.managesc.app

import android.app.Application
import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ManageSC")
                } else {
                    File(filesDir, "crash")
                }
                dir.mkdirs()
                val f = File(dir, "crash.log")
                FileWriter(f, true).use { w ->
                    w.append("=== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} ===\n")
                    w.append("Thread: ${thread.name}\n")
                    w.append(android.util.Log.getStackTraceString(throwable))
                    w.append("\n\n")
                }
            } catch (_: Exception) { }
            def?.uncaughtException(thread, throwable)
        }
    }
}
