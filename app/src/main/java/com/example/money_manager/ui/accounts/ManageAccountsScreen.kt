package com.example.money_manager.ui.accounts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.money_manager.data.model.Account
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatMoney

enum class ManageAccountsMode(val title: String) {
    SHOW_HIDE("Show/Hide Setting"),
    DELETE("Delete")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    mode: ManageAccountsMode,
    onBack: () -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val state = remember(accounts, transactions) {
        buildAccountsState(accounts, transactions, includeHidden = true)
    }
    val money = LocalMoneyColors.current
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(mode.title, fontWeight = FontWeight.SemiBold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            state.sections.forEach { section ->
                item(key = "group-${section.group}") {
                    Text(
                        text = section.group.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
                items(section.accounts, key = { it.account.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.account.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                            color = if (row.account.hidden) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (mode == ManageAccountsMode.SHOW_HIDE) {
                            Text(
                                text = row.balanceMinor.formatMoney(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    row.balanceMinor > 0L -> money.income
                                    row.balanceMinor < 0L -> money.expense
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            IconButton(onClick = { viewModel.toggleHidden(row.account) }) {
                                Icon(
                                    imageVector = if (row.account.hidden) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (row.account.hidden) "Show" else "Hide",
                                    tint = if (row.account.hidden) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        } else {
                            IconButton(onClick = { pendingDelete = row.account }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete ${row.account.name}",
                                    modifier = Modifier.size(22.dp),
                                    tint = money.expense
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
                item(key = "gap-${section.group}") { Spacer(Modifier.height(10.dp)) }
            }
        }
    }

    pendingDelete?.let { account ->
        val affected = viewModel.transactionCountFor(account.id)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text(
                    if (affected == 0) {
                        "This cannot be undone."
                    } else {
                        "$affected transaction${if (affected == 1) "" else "s"} using this " +
                            "account will also be deleted. This cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(account)
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
