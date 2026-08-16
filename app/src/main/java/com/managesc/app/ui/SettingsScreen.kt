package com.managesc.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.managesc.app.data.Prefs
import com.managesc.app.github.GitHubSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: android.content.Context, onLock: () -> Unit) {
    var token by remember { mutableStateOf(Prefs.getGhToken(context)) }
    var user by remember { mutableStateOf(Prefs.getGhUser(context)) }
    var repo by remember { mutableStateOf(Prefs.getGhRepo(context)) }
    var path by remember { mutableStateOf(Prefs.getGhPath(context)) }
    var status by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pengaturan GitHub Sync", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(), label = { Text("GitHub Token (PAT)") }, singleLine = false)
        OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
        OutlinedTextField(repo, { repo = it }, Modifier.fillMaxWidth(), label = { Text("Repo (misal: ipvps)") }, singleLine = true)
        OutlinedTextField(path, { path = it }, Modifier.fillMaxWidth(), label = { Text("Path file (misal: main/ip)") }, singleLine = true)

        Button(onClick = {
            Prefs.setGhToken(context, token)
            Prefs.setGhUser(context, user)
            Prefs.setGhRepo(context, repo)
            Prefs.setGhPath(context, path)
            syncing = true
            status = "Menyinkronkan..."
            CoroutineScope(Dispatchers.IO).launch {
                val result = GitHubSync.sync(context)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    syncing = false
                    status = result
                }
            }
        }, enabled = !syncing) {
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
            } else Text("Simpan & Sync Sekarang")
        }

        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)

        Divider()
        Text("Keamanan", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(newPin, { if (it.length <= 6) newPin = it.filter { c -> c.isDigit() } },
            Modifier.fillMaxWidth(), label = { Text("PIN Baru (6 digit)") })
        Button(onClick = {
            if (newPin.length == 6) {
                Prefs.setPin(context, newPin)
                status = "PIN diperbarui"
                newPin = ""
            }
        }) { Text("Ubah PIN") }

        Divider()
        OutlinedButton(onClick = onLock) { Text("Kunci (Logout PIN)") }
    }
}
