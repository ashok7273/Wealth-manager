package com.example.money_manager.ui.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Divider
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.ui.home.DayGroup
import com.example.money_manager.ui.home.DayHeaderRow
import com.example.money_manager.ui.home.TransactionRow
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.title
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val period by viewModel.period.collectAsState()
    val anchor by viewModel.anchor.collectAsState()
    val custom by viewModel.customRange.collectAsState()

    // Own month state so browsing here does not move the Stats screen's range.
    val initialMonth = remember(period, anchor, custom) {
        YearMonth.from(period.rangeFor(anchor, custom).end)
    }
    var month by remember(initialMonth) { mutableStateOf(initialMonth) }

    val range = remember(month) { DateRange(month.atDay(1), month.atEndOfMonth()) }
    val items = remember(transactions, range, categoryId) {
        categoryTransactions(transactions, range, categoryId)
    }
    val category = items.firstOrNull()?.category
        ?: ServiceLocator.categoryRepository.categories.value.firstOrNull { it.id == categoryId }
    val total = items.sumOf { it.amountMinor }
    val money = LocalMoneyColors.current
    val accent = if (category?.type == TransactionType.INCOME) money.income else money.expense

    val trend = remember(transactions, categoryId, month) {
        categoryMonthlyTrend(transactions, categoryId, month)
    }

    val groups = remember(items) {
        items.groupBy { it.date }
            .toSortedMap(compareByDescending<LocalDate> { it })
            .map { (date, dayItems) ->
                DayGroup(
                    date = date,
                    incomeMinor = dayItems.filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amountMinor },
                    expenseMinor = dayItems.filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountMinor },
                    items = dayItems
                )
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category?.emoji.orEmpty(), fontSize = 18.sp)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text(
                                text = category?.name ?: "Category",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(
                            Icons.Outlined.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    Text(
                        text = month.title(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(
                            Icons.Outlined.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = total.formatMoney(),
                            style = MaterialTheme.typography.titleMedium,
                            color = accent
                        )
                        Text(
                            text = if (items.size == 1) "1 entry" else "${items.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    var dragged = 0f
                    val threshold = 48.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged > threshold) month = month.minusMonths(1)
                            else if (dragged < -threshold) month = month.plusMonths(1)
                        },
                        onHorizontalDrag = { _, delta -> dragged += delta }
                    )
                },
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "trend") {
                Text(
                    text = "Last 6 months",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MonthlyTrendChart(
                    points = trend,
                    lineColor = accent,
                    highlight = month,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entries in ${month.title()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            groups.forEach { group ->
                item(key = "header-${group.date}") { DayHeaderRow(group) }
                items(group.items, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        selected = false,
                        onClick = { onEditTransaction(transaction.id) },
                        onLongClick = { onEditTransaction(transaction.id) }
                    )
                }
                item(key = "gap-${group.date}") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
