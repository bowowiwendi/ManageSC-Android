package com.managesc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.managesc.app.cloudflare.*
import com.managesc.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(context: android.content.Context, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var zones by remember { mutableStateOf<List<CfZone>>(emptyList()) }
    var selectedZone by remember { mutableStateOf<CfZone?>(null) }
    var records by remember { mutableStateOf<List<CfDnsRecord>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    fun cfgOk() = Prefs.getCfEmail(context).isNotBlank() && Prefs.getCfKey(context).isNotBlank()

    fun loadZones() {
        if (!cfgOk()) { status = "Setel Email & Global API Key di menu Stelan dulu"; return }
        val email = Prefs.getCfEmail(context); val key = Prefs.getCfKey(context)
        loading = true; status = "Memuat domain..."
        scope.launch {
            try {
                val resp = CloudflareClient.api(email, key).listZones()
                val body = resp.body()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && body?.success == true) {
                        zones = body.result ?: emptyList()
                        status = if (zones.isEmpty()) "Tidak ada domain" else "✓ ${zones.size} domain"
                    } else {
                        status = "Gagal: ${body?.errors?.firstOrNull()?.message ?: resp.code()}"
                    }
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Error: ${e.message}"; loading = false }
            }
        }
    }

    fun loadRecords(zone: CfZone) {
        val email = Prefs.getCfEmail(context); val key = Prefs.getCfKey(context)
        loading = true; status = "Memuat record ${zone.name}..."
        scope.launch {
            try {
                val resp = CloudflareClient.api(email, key).listDns(zone.id)
                val body = resp.body()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && body?.success == true) {
                        records = body.result ?: emptyList()
                        status = "✓ ${records.size} record"
                    } else {
                        status = "Gagal: ${body?.errors?.firstOrNull()?.message ?: resp.code()}"
                    }
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Error: ${e.message}"; loading = false }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding().imePadding()) {
        Text("Kelola DNS Cloudflare", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = { loadZones() },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) { Text("Muat Domain") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Kembali") }
        }

        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodySmall,
            color = if (status.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(8.dp))

        if (selectedZone == null) {
            // Daftar domain
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(zones) { z ->
                    Card(Modifier.fillMaxWidth().clickable { selectedZone = z; records = emptyList(); loadRecords(z) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(z.name, style = MaterialTheme.typography.titleMedium)
                            Text("status: ${z.status}${if (z.paused) " (paused)" else ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            // Record DNS domain terpilih
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedZone = null; records = emptyList() }) { Text("← Domain") }
                Text(selectedZone!!.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Button(onClick = { showAdd = true }, enabled = !loading) { Text("+ Record") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(label = { Text(r.type) }, onClick = {})
                                Spacer(Modifier.width(8.dp))
                                Text(r.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            }
                            Text("→ ${r.content}", style = MaterialTheme.typography.bodyMedium)
                            Text("TTL: ${if (r.ttl == 1) "Auto" else r.ttl} • Proxy: ${if (r.proxied) "ON" else "OFF"}", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showAdd = true; /* edit */ }) { Text("Edit") }
                                TextButton(onClick = {
                                    loading = true; scope.launch {
                                        CloudflareClient.api(Prefs.getCfEmail(context), Prefs.getCfKey(context)).deleteDns(selectedZone!!.id, r.id ?: "")
                                        withContext(Dispatchers.Main) { loadRecords(selectedZone!!) }
                                    }
                                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && selectedZone != null) {
        var type by remember { mutableStateOf("A") }
        var name by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var ttl by remember { mutableStateOf("1") }
        var proxied by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                Button(onClick = {
                    saving = true
                    scope.launch {
                        val rec = CfDnsRecord(
                            type = type, name = if (name.isBlank()) selectedZone!!.name else name,
                            content = content, ttl = ttl.toIntOrNull() ?: 1, proxied = proxied
                        )
                        val resp = CloudflareClient.api(Prefs.getCfEmail(context), Prefs.getCfKey(context)).createDns(selectedZone!!.id, rec)
                        withContext(Dispatchers.Main) {
                            if (resp.isSuccessful && resp.body()?.success == true) {
                                showAdd = false; loadRecords(selectedZone!!)
                            } else {
                                status = "Gagal simpan: ${resp.body()?.errors?.firstOrNull()?.message ?: resp.code()}"
                                saving = false
                            }
                        }
                    }
                }, enabled = !saving && content.isNotBlank()) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } },
            title = { Text("Tambah Record DNS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(type, { type = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("Type (A/CNAME/TXT/MX)") }, singleLine = true)
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name (subdomain, kosong=root)") }, singleLine = true)
                    OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth(), label = { Text("Content (IP/target)") }, singleLine = true)
                    OutlinedTextField(ttl, { ttl = it }, Modifier.fillMaxWidth(), label = { Text("TTL (1=Auto)") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = proxied, onCheckedChange = { proxied = it })
                        Text("Cloudflare Proxy (orange cloud)")
                    }
                }
            }
        )
    }
}
