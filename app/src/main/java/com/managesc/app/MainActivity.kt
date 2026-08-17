package com.managesc.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    // Launcher minta izin notifikasi (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user menoler/izin — tidak perlu aksi lanjut */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePref = Prefs.getTheme(this)
        // Minta izin notifikasi saat pertama jalan (Android 13+ perlu runtime request)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
