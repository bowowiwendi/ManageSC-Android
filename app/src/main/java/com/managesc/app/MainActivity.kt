package com.managesc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.managesc.app.data.Prefs
import com.managesc.app.ui.AppNav
import com.managesc.app.worker.ExpiryCheckWorker

class MainActivity : ComponentActivity() {
    // tema dipantau agar bisa diubah dari Stelan tanpa restart
    private var themePref by mutableStateOf("system")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePref = Prefs.getTheme(this)
        try {
            ExpiryCheckWorker.schedule(this)
        } catch (e: Exception) {
            android.util.Log.e("ManageSC", "schedule expiry worker failed", e)
        }
        setContent {
            val dark = when (themePref) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(context = this)
                }
            }
        }
    }

    /** Dipanggil dari SettingsScreen untuk mengubah tema live. */
    fun applyTheme(pref: String) {
        themePref = pref
        Prefs.setTheme(this, pref)
    }
}
