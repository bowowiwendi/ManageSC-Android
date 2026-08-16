package com.managesc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.managesc.app.data.Prefs
import com.managesc.app.ui.AppNav
import com.managesc.app.worker.ExpiryCheckWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Jadwalkan cek kadaluarsa harian (amankan dengan try-catch)
        try {
            ExpiryCheckWorker.schedule(this)
        } catch (e: Exception) {
            android.util.Log.e("ManageSC", "schedule expiry worker failed", e)
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val startLocked = remember { !Prefs.hasPin(this@MainActivity) }
                    var locked by mutableStateOf(startLocked)
                    AppNav(
                        context = this@MainActivity,
                        locked = locked,
                        onUnlock = { locked = false },
                        onLock = { locked = true }
                    )
                }
            }
        }
    }
}
