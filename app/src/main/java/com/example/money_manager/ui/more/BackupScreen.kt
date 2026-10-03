package com.example.money_manager.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.backup.BackupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf<BackupResult?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            result = withContext(Dispatchers.IO) {
                runCatching { ServiceLocator.backupManager.export(uri) }
                    .getOrElse { BackupResult.Failure("Export failed: ${it.message}") }
            }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) pendingImport = uri }

    // Some devices and bare emulator images ship without a documents provider.
    fun openPicker(launch: () -> Unit) {
        runCatching { launch() }.onFailure {
            result = BackupResult.Failure("No file manager app available on this device")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Backup", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Button(
                onClick = { openPicker { exportLauncher.launch(null) } },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Export to CSV", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { openPicker { importLauncher.launch(null) } },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Import from CSV", fontSize = 16.sp)
            }

            if (busy) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Working…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "What gets written",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "transactions.csv — every entry, opens in Excel",
                "accounts.csv — names, groups and opening balances",
                "categories.csv — names and order",
                "recurring.csv — active schedules"
            ).forEach { line ->
                Text(
                    text = "•  $line",
                    modifier = Modifier.padding(vertical = 3.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing removes everything currently in the app and replaces it with " +
                        "the contents of the chosen folder. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    busy = true
                    scope.launch {
                        result = withContext(Dispatchers.IO) {
                            runCatching { ServiceLocator.backupManager.import(uri) }
                                .getOrElse { BackupResult.Failure("Import failed: ${it.message}") }
                        }
                        busy = false
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            }
        )
    }

    result?.let { outcome ->
        val succeeded = outcome is BackupResult.Success
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text(if (succeeded) "Done" else "Something went wrong") },
            text = {
                Text(
                    when (outcome) {
                        is BackupResult.Success -> outcome.message
                        is BackupResult.Failure -> outcome.message
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { result = null }) { Text("OK") }
            }
        )
    }
}
