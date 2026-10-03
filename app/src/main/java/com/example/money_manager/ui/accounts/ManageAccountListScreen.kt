package com.example.money_manager.ui.accounts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

/** Accounts grouped exactly as on the Accounts tab, but for editing instead of browsing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountListScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onShowHide: () -> Unit,
    onDeleteAccounts: () -> Unit,
    onAccountClick: (Long) -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val state = remember(accounts, transactions) { buildAccountsState(accounts, transactions) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Account setting", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add") },
                                    leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                    onClick = {
                                        menuOpen = false
                                        onAddAccount()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show/Hide") },
                                    leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
                                    onClick = {
                                        menuOpen = false
                                        onShowHide()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                    onClick = {
                                        menuOpen = false
                                        onDeleteAccounts()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { innerPadding ->
        if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No visible accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                state.sections.forEach { section ->
                    item(key = "group-${section.group.id}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.group.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    items(section.accounts, key = { it.account.id }) { row ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountClick(row.account.id) }
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = row.account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (row.account.description.isNotBlank()) {
                                Text(
                                    text = row.account.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    item(key = "gap-${section.group.id}") { Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}
