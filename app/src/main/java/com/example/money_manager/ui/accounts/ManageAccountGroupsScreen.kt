package com.example.money_manager.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.model.AccountGroup
import com.example.money_manager.ui.theme.LocalMoneyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountGroupsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val money = LocalMoneyColors.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AccountGroup?>(null) }
    var pendingDelete by remember { mutableStateOf<AccountGroup?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Account Group", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add group")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pendingDelete = group }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete ${group.name}",
                            modifier = Modifier.size(22.dp),
                            tint = money.expense
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${viewModel.accountCountFor(group.id)} accounts" +
                                if (group.isCard) "  •  card billing" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { editing = group }) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit ${group.name}",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline)
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No groups yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        GroupDialog(
            title = "New group",
            initialName = "",
            initialIsCard = false,
            onDismiss = { adding = false },
            onConfirm = { name, isCard ->
                viewModel.addGroup(name, isCard)
                adding = false
            }
        )
    }

    editing?.let { group ->
        GroupDialog(
            title = "Edit group",
            initialName = group.name,
            initialIsCard = group.isCard,
            onDismiss = { editing = null },
            onConfirm = { name, isCard ->
                viewModel.updateGroup(group, name, isCard)
                editing = null
            }
        )
    }

    pendingDelete?.let { group ->
        val affected = viewModel.accountCountFor(group.id)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${group.name}?") },
            text = {
                Text(
                    if (affected == 0) {
                        "This cannot be undone."
                    } else {
                        "$affected account${if (affected == 1) "" else "s"} in this group, and " +
                            "their transactions, will also be deleted. This cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(group)
                    pendingDelete = null
                }) {
                    Text("Delete", color = money.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GroupDialog(
    title: String,
    initialName: String,
    initialIsCard: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var isCard by remember { mutableStateOf(initialIsCard) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCard = !isCard },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isCard, onCheckedChange = { isCard = it })
                    Text(
                        text = "Credit card group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, isCard) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
