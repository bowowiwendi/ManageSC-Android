package com.managesc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.managesc.app.data.VpsDbHelper
import com.managesc.app.ssh.RemoteSsh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(context: android.content.Context, id: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val vps = remember { VpsDbHelper(context).getById(id) }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Belum terhubung") }
    var statusColor by remember { mutableStateOf(MaterialTheme.colorScheme.error) }
    var lines by remember { mutableStateOf<List<AnnotatedString>>(emptyList()) }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var shell by remember { mutableStateOf<RemoteSsh.ShellSession?>(null) }
    val listState = rememberLazyListState()
    var termRows by remember { mutableStateOf(24) }
    var termCols by remember { mutableStateOf(80) }

    fun connect() {
        if (vps == null) { status = "Data VPS tidak ditemukan"; return }
        if (vps.userSsh.isBlank() || vps.passSsh.isBlank()) {
            status = "User/Password SSH kosong — isi di edit VPS"
            statusColor = MaterialTheme.colorScheme.error
            return
        }
        status = "Menghubungkan ke ${vps.ipVps} ..."; statusColor = MaterialTheme.colorScheme.tertiary
        scope.launch {
            try {
                val s = RemoteSsh.openShell(vps.ipVps, vps.userSsh, vps.passSsh, 22)
                shell = s
                connected = true
                status = "● Terhubung (port 22) — ${vps.ipVps}"
                statusColor = Color(0xFF2E7D32)
                launch(Dispatchers.IO) {
                    val buf = ByteArray(4096)
                    val acc = StringBuilder()
                    while (true) {
                        val n = try { s.output.read(buf) } catch (e: Exception) { -1 }
                        if (n <= 0) { if (!s.channel.isConnected) break else continue }
                        val chunk = String(buf, 0, n, Charsets.UTF_8)
                        acc.append(chunk)
                        if (chunk.contains('\n') || acc.length > 512) {
                            val out = acc.toString(); acc.setLength(0)
                            val rendered = parseAnsi(out)
                            withContext(Dispatchers.Main) {
                                lines = (lines + rendered).let { if (it.size > 3000) it.takeLast(1500) else it }
                                scope.launch { listState.scrollToItem(lines.size) }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) { connected = false; status = "Koneksi tertutup"; statusColor = MaterialTheme.colorScheme.error }
                }
            } catch (e: Exception) {
                status = "Gagal: ${e.message}"; statusColor = MaterialTheme.colorScheme.error
            }
        }
    }

    fun send(text: String) {
        val s = shell ?: return
        scope.launch(Dispatchers.IO) {
            try { s.input.write(text.toByteArray(Charsets.UTF_8)); s.input.flush() } catch (e: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(status, color = statusColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (!connected) Button(onClick = { connect() }) { Text("Hubungkan") }
            else OutlinedButton(onClick = onBack) { Text("Kembali") }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1117)).padding(8.dp)
                .onSizeChanged { size ->
                    val density = LocalDensity.current
                    val rows = (size.height / with(density) { 14.sp.toPx() }).toInt().coerceAtLeast(8)
                    val cols = (size.width / with(density) { 7.2.dp.toPx() }).toInt().coerceAtLeast(20)
                    termRows = rows; termCols = cols
                }
        ) {
            items(lines) { ln ->
                Text(ln, fontFamily = FontFamily.Monospace, fontSize = 12.sp, softWrap = true)
            }
        }

        // Resize PTY saat ukuran terminal berubah
        LaunchedEffect(termRows, termCols) {
            shell?.let { RemoteSsh.resizeShell(it, termCols, termRows) }
        }

        if (connected) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(4.dp)) {
                SpecialKey("ESC") { send("") }
                SpecialKey("Tab") { send("\t") }
                SpecialKey("Ctrl+C") { send("") }
                SpecialKey("↑") { send("[A") }
                SpecialKey("↓") { send("[B") }
                SpecialKey("←") { send("[D") }
                SpecialKey("→") { send("[C") }
                SpecialKey("Clear") { lines = emptyList(); send("clear\n") }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF161B22)).padding(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White
                ),
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(autoCorrect = false),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onSend = { send(input.text + "\n"); input = TextFieldValue("") }
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send(input.text + "\n"); input = TextFieldValue("") }) { Text("Kirim") }
        }
    }
}

@Composable
private fun RowScope.SpecialKey(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp).height(36.dp)) {
        Text(label, fontSize = 11.sp)
    }
}

// ANSI SGR parser ringan -> AnnotatedString dengan SpanStyle warna
fun parseAnsi(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    var fg = Color.White
    var started = -1
    while (i < text.length) {
        if (text[i] == '\u001b' && i + 1 < text.length && text[i + 1] == '[') {
            val end = text.indexOf('m', i + 2)
            if (end >= 0) {
                val code = text.substring(i + 2, end)
                val c = ansiFg(code)
                if (c != null) {
                    if (started >= 0) { builder.addStyle(SpanStyle(color = fg), started, builder.length); started = -1 }
                    fg = c
                }
                i = end + 1
                continue
            }
        }
        if (started < 0) started = builder.length
        builder.append(text[i])
        i++
    }
    if (started >= 0 && started < builder.length) builder.addStyle(SpanStyle(color = fg), started, builder.length)
    return builder.toAnnotatedString()
}

fun ansiFg(code: String): Color? {
    return when (code) {
        "0", "39" -> Color.White
        "30", "90" -> Color(0xFF9E9E9E)
        "31", "91" -> Color(0xFFEF5350)
        "32", "92" -> Color(0xFF66BB6A)
        "33", "93" -> Color(0xFFFFCA28)
        "34", "94" -> Color(0xFF42A5F5)
        "35", "95" -> Color(0xFFAB47BC)
        "36", "96" -> Color(0xFF26C6DA)
        "37", "97" -> Color(0xFFEEEEEE)
        else -> null
    }
}
