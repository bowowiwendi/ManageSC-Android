package com.managesc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import com.managesc.app.data.Prefs
import com.managesc.app.ui.AppNav
import com.managesc.app.worker.ExpiryCheckWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Jadwalkan cek kadaluarsa harian
        ExpiryCheckWorker.schedule(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val startLocked = remember { !Prefs.hasPin(this) }
                    var locked by mutableStateOf(startLocked)
                    AppNav(
                        context = this,
                        locked = locked,
                        onUnlock = { locked = false },
                        onLock = { locked = true }
                    )
                }
            }
        }
    }
}
