package com.managesc.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        PinScreen(
            context = context,
            onSuccess = onUnlock
        )
        return
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("list") { popUpTo("list") { inclusive = true } } },
                    icon = { Text("📋") },
                    label = { Text("Daftar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("add") },
                    icon = { Text("➕") },
                    label = { Text("Tambah") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("settings") },
                    icon = { Text("⚙️") },
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
                EditScreen(context = context, id = 0L, onDone = { nav.popBackStack() })
            }
            composable("edit/{id}") { back ->
                val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EditScreen(context = context, id = id, onDone = { nav.popBackStack() })
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
    var error by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (hasPin) "Masukkan PIN" else "Buat PIN 6 digit", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
            label = { Text("PIN") },
            isError = error,
            singleLine = true
        )
        if (error) Text("PIN salah", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (hasPin) {
                if (Prefs.verifyPin(context, pin)) onSuccess() else error = true
            } else {
                if (pin.length == 6) {
                    Prefs.setPin(context, pin)
                    onSuccess()
                } else error = true
            }
        }) {
            Text(if (hasPin) "Masuk" else "Simpan PIN")
        }
        if (hasPin) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                Prefs.clearPin(context)
                error = false
                pin = ""
                // force recompose ke mode setup
                android.os.Process.killProcess(android.os.Process.myPid())
            }) {
                Text("Lupa PIN? Reset (hapus semua data VPS)")
            }
        }
    }
}
