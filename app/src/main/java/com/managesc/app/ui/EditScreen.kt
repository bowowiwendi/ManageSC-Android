package com.managesc.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    var sshStatus by remember { mutableStateOf("") }

    val setupScript = com.managesc.app.Constants.SETUP_SCRIPT

    // Tipe otomatis: ada tanggal -> Limit, lifetime/kosong -> Unlimited
    fun computeTipe(ma: String): String {
        val m = ma.trim()
        return if (m.isBlank() || m.equals("lifetime", true) || m.equals("liftime", true) || m.equals("unlimited", true)) "Unlimited"
        else "Limit"
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).imePadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (id > 0) "Edit VPS" else "Tambah VPS", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
        OutlinedTextField(ip, { ip = it }, Modifier.fillMaxWidth(), label = { Text("IP VPS") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email Member") }, singleLine = true)
        OutlinedTextField(ram, { ram = it }, Modifier.fillMaxWidth(), label = { Text("RAM") }, singleLine = true)

        OutlinedTextField(masaAktif, { masaAktif = it }, Modifier.fillMaxWidth(),
            label = { Text("Masa Aktif (yyyy-MM-dd, atau 'lifetime')") }, placeholder = { Text("2026-12-31 / lifetime") }, singleLine = true)
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
                        val tipeAkun = computeTipe(masaAktif)
                        val v = Vps(
                            id = id, username = username, tipeAkun = tipeAkun, masaAktif = masaAktif,
                            ipVps = ip, emailMember = email, ram = ram, pesan = pesan,
                            userSsh = userSsh, passSsh = passSsh, serverAktif = serverAktif
                        )
                        if (id > 0) db.update(v) else db.insert(v)
                        // Jalankan setup otomatis via SSH (jika ada kredensial) di background, tidak blokir UI
                        if (ip.isNotBlank() && passSsh.isNotBlank()) {
                            scope.launch {
                                RemoteSsh.runCommand(ip, userSsh.ifBlank { "root" }, passSsh, setupScript)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context, "Tersimpan", android.widget.Toast.LENGTH_SHORT
                            ).show()
                            onDone()
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
}
