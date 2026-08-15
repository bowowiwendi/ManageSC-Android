package com.managesc.app.ui

import androidx.compose.foundation.layout.*
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
fun EditScreen(context: android.content.Context, id: Long, onDone: () -> Unit) {
    val db = remember { VpsDbHelper(context) }
    val existing = remember { if (id > 0) db.getById(id) else null }

    var username by remember { mutableStateOf(existing?.username ?: "") }
    var tipe by remember { mutableStateOf(existing?.tipeAkun ?: "") }
    var masaAktif by remember { mutableStateOf(existing?.masaAktif ?: "") }
    var ip by remember { mutableStateOf(existing?.ipVps ?: "") }
    var email by remember { mutableStateOf(existing?.emailMember ?: "") }
    var ram by remember { mutableStateOf(existing?.ram ?: "") }
    var pesan by remember { mutableStateOf(existing?.pesan ?: "") }

    Column(Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (id > 0) "Edit VPS" else "Tambah VPS", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") })
        OutlinedTextField(tipe, { tipe = it }, Modifier.fillMaxWidth(), label = { Text("Tipe Akun") })
        OutlinedTextField(masaAktif, { masaAktif = it }, Modifier.fillMaxWidth(), label = { Text("Masa Aktif (yyyy-MM-dd)") }, placeholder = { Text("2026-12-31") })
        OutlinedTextField(ip, { ip = it }, Modifier.fillMaxWidth(), label = { Text("IP VPS") })
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email Member") })
        OutlinedTextField(ram, { ram = it }, Modifier.fillMaxWidth(), label = { Text("RAM") })
        OutlinedTextField(pesan, { pesan = it }, Modifier.fillMaxWidth().height(80.dp), label = { Text("Pesan") }, singleLine = false)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val v = Vps(
                    id = id,
                    username = username,
                    tipeAkun = tipe,
                    masaAktif = masaAktif,
                    ipVps = ip,
                    emailMember = email,
                    ram = ram,
                    pesan = pesan
                )
                if (id > 0) db.update(v) else db.insert(v)
                onDone()
            }, Modifier.weight(1f)) { Text("Simpan") }

            if (id > 0) {
                OutlinedButton(onClick = { db.delete(id); onDone() }) { Text("Hapus") }
            }
        }

        if (id > 0) {
            OutlinedButton(onClick = {
                // Renew: +30 hari dari masa aktif sekarang (atau hari ini)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val base = try { sdf.parse(masaAktif) } catch (_: Exception) { Date() } ?: Date()
                val cal = Calendar.getInstance().apply { time = base; add(Calendar.DAY_OF_MONTH, 30) }
                masaAktif = sdf.format(cal.time)
            }) { Text("Perpanjang +30 hari") }
        }
    }
}
