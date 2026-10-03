package com.example.money_manager.ui.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatAmount
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.formatSigned
import com.example.money_manager.util.monthYearLabel
import com.example.money_manager.util.title
import com.example.money_manager.util.weekdayShort
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val account = accounts.firstOrNull { it.id == accountId }

    var tab by remember { mutableStateOf(StatementTab.DAILY) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val drillHistory = remember { mutableStateListOf<StatementTab>() }

    if (account == null) {
        onBack()
        return
    }

    val statement = remember(account, transactions, tab, month) {
        when (tab) {
            StatementTab.DAILY -> buildDailyStatement(account, transactions, month)
            StatementTab.MONTHLY -> buildMonthlyStatement(account, transactions, month.year)
            StatementTab.ANNUALLY -> buildAnnualStatement(account, transactions)
        }
    }
    val money = LocalMoneyColors.current

    fun shift(direction: Int) {
        month = when (tab) {
            StatementTab.DAILY -> month.plusMonths(direction.toLong())
            StatementTab.MONTHLY -> month.plusYears(direction.toLong())
            StatementTab.ANNUALLY -> month
        }
    }

    fun drillInto(row: StatementPeriodRow) {
        drillHistory.add(tab)
        if (row.monthValue != null) {
            month = YearMonth.of(row.year, row.monthValue)
            tab = StatementTab.DAILY
        } else {
            month = month.withYear(row.year)
            tab = StatementTab.MONTHLY
        }
    }

    // Back steps out of a drill-down before leaving the screen.
    BackHandler(enabled = drillHistory.isNotEmpty()) {
        tab = drillHistory.removeAt(drillHistory.lastIndex)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransaction(account.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
            }
        },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text(account.name, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (tab == StatementTab.ANNUALLY) {
                            Text(
                                text = annualTitle(account, transactions),
                                modifier = Modifier.padding(end = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            IconButton(onClick = { shift(-1) }) {
                                Icon(
                                    Icons.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Previous"
                                )
                            }
                            Text(
                                text = if (tab == StatementTab.DAILY) {
                                    month.title()
                                } else {
                                    month.year.toString()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { shift(1) }) {
                                Icon(
                                    Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Next"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                StatementTabs(
                    selected = tab,
                    onSelect = {
                        drillHistory.clear()
                        tab = it
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Statement",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = statement.rangeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onEditAccount(account.id) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit account")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    SummaryCell("Deposit", statement.depositMinor.formatAmount(), money.income)
                    SummaryCell("Withdrawal", statement.withdrawalMinor.formatAmount(), money.expense)
                    SummaryCell(
                        "Total",
                        statement.totalMinor.formatSigned(),
                        MaterialTheme.colorScheme.onSurface
                    )
                    if (tab != StatementTab.ANNUALLY) {
                        SummaryCell(
                            "Balance",
                            statement.closingBalanceMinor.formatSigned(),
                            if (statement.closingBalanceMinor < 0L) money.expense else money.income
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(tab) {
                if (tab == StatementTab.ANNUALLY) return@pointerInput
                var dragged = 0f
                val threshold = 48.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        if (dragged > threshold) shift(-1) else if (dragged < -threshold) shift(1)
                    },
                    onHorizontalDrag = { _, delta -> dragged += delta }
                )
            }

        LazyColumn(modifier = content, contentPadding = PaddingValues(bottom = 24.dp)) {
            if (tab == StatementTab.DAILY) {
                if (statement.days.isEmpty()) {
                    item(key = "empty") { EmptyStatement() }
                }
                statement.days.forEach { day ->
                    item(key = "day-${day.date}") {
                        StatementDayHeader(day = day)
                    }
                    items(day.entries, key = { it.transaction.id }) { entry ->
                        StatementEntryRow(
                            entry = entry,
                            onClick = { onEditTransaction(entry.transaction.id) }
                        )
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                    item(key = "gap-${day.date}") { Spacer(Modifier.height(8.dp)) }
                }
            } else {
                if (statement.periods.isEmpty()) {
                    item(key = "empty") { EmptyStatement() }
                }
                items(statement.periods, key = { it.title }) { period ->
                    StatementPeriodItem(row = period, onClick = { drillInto(period) })
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun EmptyStatement() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No activity in this period",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SummaryCell(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = valueColor
        )
    }
}

@Composable
private fun StatementTabs(selected: StatementTab, onSelect: (StatementTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        StatementTab.values().forEach { entry ->
            val isSelected = entry == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(entry) }
            ) {
                Text(
                    text = entry.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun StatementDayHeader(day: StatementDay) {
    val money = LocalMoneyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.date.dayOfMonth.toString().padStart(2, '0'),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = day.date.weekdayShort(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = day.date.monthYearLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = day.depositMinor.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (day.depositMinor == 0L) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                money.income
            }
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = day.withdrawalMinor.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (day.withdrawalMinor == 0L) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                money.expense
            }
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun StatementEntryRow(entry: StatementEntry, onClick: () -> Unit) {
    val money = LocalMoneyColors.current
    val transaction = entry.transaction
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transaction.category?.name ?: "Transfer",
            modifier = Modifier.width(84.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { transaction.category?.name ?: "Transfer" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transaction.account.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = kotlin.math.abs(entry.deltaMinor).formatMoney(),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.deltaMinor >= 0L) money.income else money.expense
            )
            Text(
                text = "(Balance ${entry.balanceAfterMinor.formatSigned()})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatementPeriodItem(row: StatementPeriodRow, onClick: () -> Unit) {
    val money = LocalMoneyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (row.isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = row.rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.depositMinor.formatMoney(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (row.depositMinor == 0L) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    money.income
                }
            )
            Text(
                text = row.totalMinor.formatSigned(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.withdrawalMinor.formatMoney(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (row.withdrawalMinor == 0L) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    money.expense
                }
            )
            Text(
                text = "(${row.closingBalanceMinor.formatSigned()})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
