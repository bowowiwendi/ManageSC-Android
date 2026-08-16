package com.managesc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.managesc.app.ui.AppNav
import com.managesc.app.worker.ExpiryCheckWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ExpiryCheckWorker.schedule(this)
        } catch (e: Exception) {
            android.util.Log.e("ManageSC", "schedule expiry worker failed", e)
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(context = this)
                }
            }
        }
    }
}
