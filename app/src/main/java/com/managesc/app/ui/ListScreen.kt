package com.managesc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
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
fun ListScreen(context: android.content.Context, onEdit: (Long) -> Unit, onRemote: (Long) -> Unit) {
    var items by remember { mutableStateOf(emptyList<Vps>()) }
    var query by remember { mutableStateOf("") }
    var reach by remember { mutableStateOf(mapOf<Long, Boolean?>()) } // null = cek, true = hidup, false = mati

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

    val filtered = items.filter {
        it.username.contains(query, true) || it.ipVps.contains(query, true)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari username / IP") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onRemote(v.id) }, modifier = Modifier.width(96.dp)) {
                            Text("Remote", fontSize = MaterialTheme.typography.labelSmall.fontSize)
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
