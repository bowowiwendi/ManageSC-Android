package com.managesc.app

import android.app.Application
import android.os.Environment
import android.util.Log
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
            val msg = android.util.Log.getStackTraceString(throwable)
            Log.e("ManageSC_CRASH", msg)
            try {
                // Internal storage (no permission needed)
                val f = File(filesDir, "crash.log")
                FileWriter(f, true).use { w ->
                    w.append("=== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} ===\n")
                    w.append("Thread: ${thread.name}\n")
                    w.append(msg)
                    w.append("\n\n")
                }
                // Also try Download (may fail on some Android versions — ignore)
                try {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "ManageSC"
                    )
                    dir.mkdirs()
                    FileWriter(File(dir, "crash.log"), true).use { w ->
                        w.append("=== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} ===\n")
                        w.append(msg)
                        w.append("\n\n")
                    }
                } catch (_: Exception) { }
            } catch (_: Exception) { }
            def?.uncaughtException(thread, throwable)
        }
    }
}
