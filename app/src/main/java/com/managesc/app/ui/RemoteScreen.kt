package com.managesc.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.managesc.app.data.VpsDbHelper
import com.managesc.app.ssh.RemoteSsh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(context: android.content.Context, id: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val v = remember { VpsDbHelper(context).getById(id) }
    var cmd by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<String>() }
    val port = 22

    val setupScript = "sysctl net.ipv6.conf.all.disable_ipv6=1 && sysctl net.ipv6.conf.default.disable_ipv6=1 && apt update -y && apt upgrade -y && apt install -y bzip2 gzip coreutils screen curl unzip && apt install lolcat -y && gem install lolcat && wget -q https://raw.githubusercontent.com/bowowiwendi/WendyVpn/ABSTRAK/setup-main.sh && chmod +x setup-main.sh && sed -i -e 's/\\$//' setup-main.sh && screen -S setupku ./setup-main.sh"

    fun append(line: String) { lines.add(line) }

    fun runCmd(c: String) {
        if (v == null) return
        busy = true
        scope.launch {
            append("$ c")
            val r = RemoteSsh.runCommand(v.ipVps, v.userSsh.ifBlank { "root" }, v.passSsh, c)
            append(r)
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (v != null) {
            append("Menghubungkan ke ${v.ipVps}:$port sebagai ${v.userSsh.ifBlank { "root" }} ...")
            val r = RemoteSsh.testConnection(v.ipVps, v.userSsh.ifBlank { "root" }, v.passSsh, port)
            if (r.startsWith("Koneksi SSH berhasil")) {
                connected = true
                append("● Terhubung (port 22)")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Terhubung ke ${v.ipVps}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                append("✗ $r")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Gagal terhubung", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Remote: ${v?.username} (${v?.ipVps})", style = MaterialTheme.typography.titleMedium)
        Text(
            if (connected) "● Terhubung (port 22)" else "○ Belum terhubung",
            style = MaterialTheme.typography.labelMedium,
            color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            tonalElevation = 2.dp
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1) }
            LazyColumn(Modifier.padding(8.dp).fillMaxSize(), state = listState) {
                items(lines) { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(cmd, { cmd = it }, Modifier.fillMaxWidth(), label = { Text("Perintah (mis: uptime)") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        // Baris tombol perintah
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { if (cmd.isNotBlank()) runCmd(cmd) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Kirim") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (v != null) { busy = true; scope.launch { append("$ menjalankan setup otomatis ..."); val r = RemoteSsh.runCommand(v.ipVps, v.userSsh.ifBlank { "root" }, v.passSsh, setupScript); append(r); busy = false } } },
                enabled = !busy, modifier = Modifier.weight(1f)
            ) { Text("Setup Otomatis") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Kembali") }
        }
        Spacer(Modifier.height(8.dp))
        // Tombol khusus ala Termux
        Text("Tombol khusus:", style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            val keys = listOf(
                "ESC" to "\u001b", "Tab" to "\t", "Ctrl+C" to "\u0003",
                "↑" to "\u001b[A", "↓" to "\u001b[B", "←" to "\u001b[D", "→" to "\u001b[C",
                "Clear" to "clear\n"
            )
            keys.forEach { (label, seq) ->
                OutlinedButton(
                    onClick = {
                        if (label == "Clear") runCmd("clear")
                        else cmd = cmd + seq
                    },
                    modifier = Modifier.padding(end = 6.dp)
                ) { Text(label) }
            }
        }
    }
}
