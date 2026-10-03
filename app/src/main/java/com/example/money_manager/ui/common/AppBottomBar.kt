package com.example.money_manager.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class AppDestination(val label: String) {
    TRANSACTIONS("Trans."),
    STATS("Stats"),
    ACCOUNTS("Accounts"),
    MORE("More")
}

@Composable
fun AppBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        AppDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = when (destination) {
                            AppDestination.TRANSACTIONS -> Icons.Outlined.ReceiptLong
                            AppDestination.STATS -> Icons.Outlined.BarChart
                            AppDestination.ACCOUNTS -> Icons.Outlined.AccountBalanceWallet
                            AppDestination.MORE -> Icons.Outlined.MoreHoriz
                        },
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
