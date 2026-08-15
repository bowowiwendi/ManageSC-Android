package com.managesc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    LaunchedEffect(Unit) { items = VpsDbHelper(context).getAll() }

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
                    Column(Modifier.padding(12.dp)) {
                        Text(v.username, style = MaterialTheme.typography.titleMedium)
                        Text("IP: ${v.ipVps}", style = MaterialTheme.typography.bodySmall)
                        Text("Tipe: ${v.tipeAkun} • RAM: ${v.ram}", style = MaterialTheme.typography.bodySmall)
                        val days = expiryDays(v.masaAktif)
                        val color = if (days <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        Text("Kadaluarsa: ${v.masaAktif} (${if (days<0) "lewat" else "$days hari"})",
                            style = MaterialTheme.typography.bodySmall, color = color)
                    }
                }
            }
        }
        if (filtered.isEmpty()) Text("Tidak ada data", Modifier.padding(16.dp))
    }
}

fun expiryDays(dateStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val exp = sdf.parse(dateStr) ?: return -1
        ((exp.time - Calendar.getInstance().timeInMillis) / (1000*60*60*24)).toInt()
    } catch (_: Exception) { -1 }
}
