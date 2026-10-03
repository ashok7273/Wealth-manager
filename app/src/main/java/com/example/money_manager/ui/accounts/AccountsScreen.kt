package com.example.money_manager.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.ui.common.AppBottomBar
import com.example.money_manager.ui.common.AppDestination
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatAmount
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.formatSigned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigate: (AppDestination) -> Unit,
    onAddAccount: () -> Unit,
    onShowHide: () -> Unit,
    onDeleteAccounts: () -> Unit,
    onOpenStats: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onPayCard: (Long) -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val state = remember(accounts, transactions) { buildAccountsState(accounts, transactions) }

    val money = LocalMoneyColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Accounts", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onOpenStats) {
                            Icon(Icons.Outlined.BarChart, contentDescription = "Total stats")
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    NetCell("Assets", state.assetsMinor.formatAmount(), money.income, Modifier.weight(1f))
                    NetCell(
                        "Liabilities",
                        state.liabilitiesMinor.formatAmount(),
                        money.expense,
                        Modifier.weight(1f)
                    )
                    NetCell(
                        "Total",
                        state.totalMinor.formatSigned(),
                        if (state.totalMinor < 0) money.expense else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        },
        bottomBar = {
            AppBottomBar(selected = AppDestination.ACCOUNTS, onSelect = onNavigate)
        }
    ) { innerPadding ->
        if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83C\uDFE6", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No visible accounts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add one from the menu above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                state.sections.forEach { section ->
                    item(key = "group-${section.group}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.group.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = section.totalMinor.formatMoney(),
                                style = MaterialTheme.typography.titleMedium,
                                color = balanceColor(section.totalMinor)
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    items(section.accounts, key = { it.account.id }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountClick(row.account.id) }
                                .padding(
                                    start = 16.dp,
                                    end = if (row.account.group.isCard) 8.dp else 16.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            Text(
                                text = row.balanceMinor.formatMoney(),
                                style = MaterialTheme.typography.titleMedium,
                                color = balanceColor(row.balanceMinor)
                            )
                            if (row.account.group.isCard) {
                                Spacer(Modifier.width(8.dp))
                                PayButton(onClick = { onPayCard(row.account.id) })
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    item(key = "gap-${section.group}") { Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PayButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(17.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = "Pay", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun balanceColor(amountMinor: Long): Color {
    val money = LocalMoneyColors.current
    return when {
        amountMinor > 0L -> money.income
        amountMinor < 0L -> money.expense
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun NetCell(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = valueColor
        )
    }
}
