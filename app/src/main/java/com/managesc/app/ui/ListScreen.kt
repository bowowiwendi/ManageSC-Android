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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(context: android.content.Context, onEdit: (Long) -> Unit) {
    var items by remember { mutableStateOf(emptyList<Vps>()) }
    var query by remember { mutableStateOf("") }

    fun reload() { items = VpsDbHelper(context).getAll() }
    LaunchedEffect(Unit) { reload() }

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
                Card(Modifier.fillMaxWidth().clickable { onEdit(v.id) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Lampu status
                        Box(
                            Modifier.size(12.dp).clip(CircleShape).background(
                                if (v.serverAktif) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
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
    if (v.tipeAkun.equals("Unlimited", true) || v.masaAktif.isBlank()) {
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
