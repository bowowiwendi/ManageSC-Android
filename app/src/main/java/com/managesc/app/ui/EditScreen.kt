package com.managesc.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.managesc.app.data.Vps
import com.managesc.app.data.VpsDbHelper
import com.managesc.app.ssh.RemoteSsh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(context: android.content.Context, id: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { VpsDbHelper(context) }
    val existing = remember { if (id > 0) db.getById(id) else null }

    var username by remember { mutableStateOf(existing?.username ?: "") }
    var tipe by remember { mutableStateOf(existing?.tipeAkun ?: "Limit") }
    var tipeExpanded by remember { mutableStateOf(false) }
    var masaAktif by remember { mutableStateOf(existing?.masaAktif ?: "") }
    var ip by remember { mutableStateOf(existing?.ipVps ?: "") }
    var email by remember { mutableStateOf(existing?.emailMember ?: "") }
    var ram by remember { mutableStateOf(existing?.ram ?: "") }
    var pesan by remember { mutableStateOf(existing?.pesan ?: "") }
    var userSsh by remember { mutableStateOf(existing?.userSsh ?: "") }
    var passSsh by remember { mutableStateOf(existing?.passSsh ?: "") }
    var serverAktif by remember { mutableStateOf(existing?.serverAktif ?: true) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showScriptDialog by remember { mutableStateOf(false) }
    var sshStatus by remember { mutableStateOf("") }

    val tipeOptions = listOf("Limit", "Unlimited")
    val setupScript = """sysctl net.ipv6.conf.all.disable_ipv6=1 && sysctl net.ipv6.conf.default.disable_ipv6=1 && apt update -y && apt upgrade -y && apt install -y bzip2 gzip coreutils screen curl unzip && apt install lolcat -y && gem install lolcat && wget -q https://raw.githubusercontent.com/bowowiwendi/WendyVpn/ABSTRAK/setup-main.sh && chmod +x setup-main.sh && sed -i -e 's/\$//' setup-main.sh && screen -S setupku ./setup-main.sh"""

    Column(
        Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding().imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (id > 0) "Edit VPS" else "Tambah VPS", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
        OutlinedTextField(ip, { ip = it }, Modifier.fillMaxWidth(), label = { Text("IP VPS") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email Member") }, singleLine = true)
        OutlinedTextField(ram, { ram = it }, Modifier.fillMaxWidth(), label = { Text("RAM") }, singleLine = true)

        ExposedDropdownMenuBox(expanded = tipeExpanded, onExpandedChange = { tipeExpanded = it }) {
            OutlinedTextField(
                value = tipe, onValueChange = {}, readOnly = true, label = { Text("Tipe Akun") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipeExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = tipeExpanded, onDismissRequest = { tipeExpanded = false }) {
                tipeOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { tipe = opt; tipeExpanded = false })
                }
            }
        }

        OutlinedTextField(masaAktif, { masaAktif = it }, Modifier.fillMaxWidth(),
            label = { Text("Masa Aktif (yyyy-MM-dd, kosong=Unlimited)") }, placeholder = { Text("2026-12-31") }, singleLine = true)
        OutlinedTextField(userSsh, { userSsh = it }, Modifier.fillMaxWidth(), label = { Text("User SSH (remote)") }, singleLine = true)
        OutlinedTextField(passSsh, { passSsh = it }, Modifier.fillMaxWidth(), label = { Text("Password SSH (remote)") }, singleLine = true)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = serverAktif, onCheckedChange = { serverAktif = it })
            Text("Server aktif (lampu hijau)")
        }

        Button(
            onClick = {
                sshStatus = "Mengetes koneksi..."
                scope.launch {
                    val r = RemoteSsh.testConnection(ip, userSsh.ifBlank { "root" }, passSsh)
                    sshStatus = r
                }
            },
            enabled = ip.isNotBlank() && passSsh.isNotBlank() && sshStatus != "Mengetes koneksi...",
            modifier = Modifier.fillMaxWidth()
        ) { Text("Test Koneksi SSH") }

        if (sshStatus.isNotBlank()) {
            Text(sshStatus, style = MaterialTheme.typography.bodySmall,
                color = if (sshStatus.startsWith("Koneksi SSH berhasil")) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(pesan, { pesan = it }, Modifier.fillMaxWidth().height(80.dp), label = { Text("Pesan") }, singleLine = false)

        if (errorMsg.isNotBlank()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                if (username.isBlank()) { errorMsg = "Username wajib diisi"; return@Button }
                saving = true
                errorMsg = ""
                scope.launch {
                    try {
                        val v = Vps(
                            id = id, username = username, tipeAkun = tipe, masaAktif = masaAktif,
                            ipVps = ip, emailMember = email, ram = ram, pesan = pesan,
                            userSsh = userSsh, passSsh = passSsh, serverAktif = serverAktif
                        )
                        if (id > 0) db.update(v) else db.insert(v)
                        withContext(Dispatchers.Main) {
                            if (ip.isNotBlank()) showScriptDialog = true else onDone()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { saving = false; errorMsg = "Gagal simpan: ${e.message ?: e.javaClass.simpleName}" }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Simpan")
        }

        if (id > 0) {
            OutlinedButton(
                onClick = {
                    saving = true
                    scope.launch {
                        try { db.delete(id); withContext(Dispatchers.Main) { onDone() } }
                        catch (e: Exception) { withContext(Dispatchers.Main) { saving = false; errorMsg = "Gagal hapus: ${e.message}" } }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Hapus") }
        }

        if (id > 0) {
            OutlinedButton(
                onClick = {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val base = try { sdf.parse(masaAktif) } catch (_: Exception) { java.util.Date() } ?: java.util.Date()
                    val cal = java.util.Calendar.getInstance().apply { time = base; add(java.util.Calendar.DAY_OF_MONTH, 30) }
                    masaAktif = sdf.format(cal.time)
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Perpanjang +30 hari") }
        }
    }

    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false; onDone() },
            confirmButton = {
                TextButton(onClick = {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("SetupVPS", setupScript))
                    showScriptDialog = false
                    onDone()
                }) { Text("Salin Skrip") }
            },
            dismissButton = { TextButton(onClick = { showScriptDialog = false; onDone() }) { Text("Tutup") } },
            title = { Text("Simpan Berhasil") },
            text = {
                Column {
                    Text("IP VPS tersimpan. Jalankan skrip ini di server VPS untuk setup otomatis:")
                    Spacer(Modifier.height(8.dp))
                    Text(setupScript, style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}
