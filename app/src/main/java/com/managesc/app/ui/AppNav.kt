package com.managesc.app.ui

import android.os.Process
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import com.managesc.app.data.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(
    context: android.content.Context,
    locked: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit
) {
    val nav = rememberNavController()
    if (locked) {
        PinScreen(context = context, onSuccess = onUnlock)
        return
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("list") { popUpTo("list") { inclusive = true } } },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Daftar") },
                    label = { Text("Daftar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("add") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Tambah") },
                    label = { Text("Tambah") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("settings") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Setelan") },
                    label = { Text("Setelan") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "list",
            modifier = Modifier.padding(padding)
        ) {
            composable("list") {
                ListScreen(context = context, onEdit = { id -> nav.navigate("edit/$id") })
            }
            composable("add") {
                EditScreen(context = context, id = 0L, onDone = { nav.navigate("list") { popUpTo("list") { inclusive = true } } })
            }
            composable("edit/{id}") { back ->
                val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EditScreen(context = context, id = id, onDone = { nav.navigate("list") { popUpTo("list") { inclusive = true } } })
            }
            composable("settings") {
                SettingsScreen(context = context, onLock = onLock)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(context: android.content.Context, onSuccess: () -> Unit) {
    val hasPin = Prefs.hasPin(context)
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (hasPin) "Masukkan PIN" else if (showConfirm) "Konfirmasi PIN" else "Buat PIN 6 digit",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = if (showConfirm) pinConfirm else pin,
                onValueChange = {
                    val cleaned = it.filter { c -> c.isDigit() }.take(6)
                    if (showConfirm) pinConfirm = cleaned else pin = cleaned
                    error = ""
                },
                label = { Text(if (showConfirm) "Ulangi PIN" else "PIN") },
                isError = error.isNotBlank(),
                singleLine = true,
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (hasPin) {
                        checking = true
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (Prefs.verifyPin(context, pin)) onSuccess() else {
                                error = "PIN salah"
                                checking = false
                                pin = ""
                            }
                        }, 400)
                    } else {
                        if (!showConfirm) {
                            if (pin.length == 6) { showConfirm = true; pinConfirm = "" }
                            else error = "PIN harus 6 digit"
                        } else {
                            if (pinConfirm.length == 6 && pinConfirm == pin) {
                                Prefs.setPin(context, pin)
                                onSuccess()
                            } else if (pinConfirm.length == 6) {
                                error = "PIN tidak cocok"
                                pinConfirm = ""
                            } else error = "PIN harus 6 digit"
                        }
                    }
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                if (checking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (hasPin) "Masuk" else if (showConfirm) "Konfirmasi" else "Simpan PIN")
                }
            }
            if (hasPin) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    Prefs.clearPin(context)
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) {
                    Text("Lupa PIN? Reset (hapus semua data VPS)")
                }
            }
        }
    }
}
