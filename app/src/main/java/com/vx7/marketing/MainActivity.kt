package com.vx7.marketing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val vm: Vm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) }
    }
}

@Composable
fun App(vm: Vm) {
    val dark = when (vm.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    AppTheme(dark) {
        // Back stack is stored as a "|"-joined string so it survives rotation and process recreation.
        var stackStr by rememberSaveable { mutableStateOf("home") }
        val stack = stackStr.split("|")
        val route = stack.last()
        fun go(r: String) { stackStr = "$stackStr|$r" }
        fun back() { if (stack.size > 1) stackStr = stack.dropLast(1).joinToString("|") }
        fun replace(r: String) { stackStr = (stack.dropLast(1) + r).joinToString("|") }
        BackHandler(enabled = stack.size > 1) { back() }

        val snack = remember { SnackbarHostState() }
        LaunchedEffect(vm.message) {
            vm.message?.let { snack.showSnackbar(it); vm.message = null }
        }
        val title = when {
            route == "home" -> "VX7 Marketing"
            route == "products" -> "Products"
            route == "form" -> if (vm.editingId == 0L) "New Collection Record" else "Edit Collection Record"
            route.startsWith("detail:") -> "Record Details"
            route == "history" -> "Collection History"
            route == "reports" -> "Reports"
            route == "backup" -> "Backup & Restore"
            route == "settings" -> "Settings"
            route == "about" -> "About VX7 Marketing"
            route == "notes" -> "Notes"
            else -> "VX7 Marketing"
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { Bar(title, if (stack.size > 1) ({ back() }) else null) },
            snackbarHost = { SnackbarHost(snack) }
        ) { pad ->
            when {
                route == "home" -> HomeScreen(vm, pad, ::go)
                route == "products" -> ProductsScreen(vm, pad)
                route == "form" -> FormScreen(vm, pad, ::go, ::back, ::replace)
                route.startsWith("detail:") -> DetailScreen(vm, route.substringAfter(":").toLongOrNull() ?: 0L, pad, ::go, ::back)
                route == "history" -> HistoryScreen(vm, pad, ::go)
                route == "reports" -> ReportsScreen(vm, pad, ::go)
                route == "backup" -> BackupScreen(vm, pad)
                route == "settings" -> SettingsScreen(vm, pad, ::go)
                route == "about" -> AboutScreen(pad)
                route == "notes" -> NotesScreen(vm, pad)
                else -> HomeScreen(vm, pad, ::go)
            }
        }
    }
}
