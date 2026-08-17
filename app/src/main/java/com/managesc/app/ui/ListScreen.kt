package com.managesc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.managesc.app.data.Vps
import com.managesc.app.data.VpsDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(context: android.content.Context, onEdit: (Long) -> Unit, onRemote: (Long) -> Unit, onSettings: () -> Unit) {
    var items by remember { mutableStateOf(emptyList<Vps>()) }
    var query by remember { mutableStateOf("") }
    var reach by remember { mutableStateOf(mapOf<Long, Boolean?>()) } // null = cek, true = hidup, false = mati
    var setupBusy by remember { mutableStateOf<Long?>(null) }
    var setupMsg by remember { mutableStateOf("") }

    fun reload() { items = VpsDbHelper(context).getAll() }
    LaunchedEffect(Unit) { reload() }

    // Cek reachability tiap item (ping)
    LaunchedEffect(items) {
        val scope = this
        items.forEach { v ->
            if (v.ipVps.isNotBlank()) {
                launch(Dispatchers.IO) {
                    val alive = try {
                        InetAddress.getByName(v.ipVps).isReachable(3000)
                    } catch (_: Exception) { false }
                    withContext(Dispatchers.Main) {
                        reach = reach + (v.id to alive)
                    }
                }
            }
        }
    }

    fun runSetup(v: Vps) {
        if (v.userSsh.isBlank() || v.passSsh.isBlank()) { setupMsg = "User/Password SSH kosong untuk ${v.username}"; return }
        setupBusy = v.id; setupMsg = "Menjalankan setup di ${v.username}..."
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val res = RemoteSsh.runSetup(v.ipVps, v.userSsh, v.passSsh, com.managesc.app.Constants.SETUP_SCRIPT)
            withContext(Dispatchers.Main) {
                setupBusy = null
                setupMsg = "Setup ${v.username}: ${res.take(200)}"
            }
        }
    }

    val filtered = items.filter {
        it.username.contains(query, true) || it.ipVps.contains(query, true)
    }

    Column(Modifier.fillMaxSize().padding(12.dp).imePadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari username / IP") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Setelan")
            }
        }
        if (setupMsg.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(setupMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val alive = reach[v.id]
                            Box(
                                Modifier.size(12.dp).clip(CircleShape).background(
                                    when (alive) {
                                        true -> Color(0xFF4CAF50)
                                        false -> Color(0xFFE53935)
                                        null -> Color(0xFF9E9E9E)
                                    }
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).clickable { onEdit(v.id) }) {
                                Text(v.username, style = MaterialTheme.typography.titleMedium)
                                Text("IP: ${v.ipVps}", style = MaterialTheme.typography.bodySmall)
                                Text("Tipe: ${v.tipeAkun} • RAM: ${v.ram}", style = MaterialTheme.typography.bodySmall)
                                val info = expiryInfo(v)
                                val color = when {
                                    v.tipeAkun.equals("Unlimited", true) -> MaterialTheme.colorScheme.primary
                                    info.days <= 7 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(info.text, style = MaterialTheme.typography.bodySmall, color = color)
                                Text(
                                    if (alive == true) "● Server hidup (ping OK)"
                                    else if (alive == false) "○ Server mati/tidak reachable"
                                    else "○ Mengecek...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (alive) { true -> Color(0xFF4CAF50); false -> Color(0xFFE53935); else -> MaterialTheme.colorScheme.outline }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Button(onClick = { onRemote(v.id) }, modifier = Modifier.weight(1f)) {
                                Text("Remote")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { runSetup(v) },
                                enabled = setupBusy == null,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (setupBusy == v.id) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text("Setup Otomatis")
                            }
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) Text("Tidak ada data", Modifier.padding(16.dp))
    }
}

data class ExpiryInfo(val days: Int, val text: String)

fun expiryInfo(v: Vps): ExpiryInfo {
    if (v.tipeAkun.equals("Unlimited", true) || v.tipeAkun.equals("lifetime", true)
        || v.tipeAkun.equals("liftime", true) || v.masaAktif.isBlank()) {
        return ExpiryInfo(Int.MAX_VALUE, "Unlimited (tidak kadaluarsa)")
    }
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val exp = sdf.parse(v.masaAktif) ?: return ExpiryInfo(-1, "Kadaluarsa: ${v.masaAktif}")
        val days = ((exp.time - Calendar.getInstance().timeInMillis) / (1000*60*60*24)).toInt()
        ExpiryInfo(days, "Kadaluarsa: ${v.masaAktif} (${if (days<0) "lewat" else "$days hari"})")
    } catch (_: Exception) {
        ExpiryInfo(-1, "Kadaluarsa: ${v.masaAktif}")
    }
}
