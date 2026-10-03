package com.example.money_manager.ui.filter

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.model.TransactionFilter
import com.example.money_manager.ui.home.HomeViewModel
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.formatSigned
import com.example.money_manager.util.title
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    onClose: () -> Unit,
    onApply: (TransactionFilter) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by ServiceLocator.accountRepository.accounts.collectAsState()
    val categories by ServiceLocator.categoryRepository.categories.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val applied by viewModel.filter.collectAsState()

    var draft by remember { mutableStateOf(applied) }
    var tab by remember { mutableStateOf(FilterTab.EXPENSES) }

    val state = remember(transactions, accounts, categories, month, draft) {
        buildFilterState(transactions, accounts, categories, month, draft)
    }
    val money = LocalMoneyColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectMonth(month.minusMonths(1)) }) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = month.title(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.selectMonth(month.plusMonths(1)) }) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next month")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close filter")
                    }
                }
                FilterBanner(
                    draft = draft,
                    onClear = { draft = TransactionFilter() },
                    onApply = { onApply(draft) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PercentRing(
                    label = "Income",
                    fraction = state.incomeFraction,
                    amount = state.displayIncomeMinor,
                    color = money.income,
                    modifier = Modifier.weight(1f)
                )
                PercentRing(
                    label = "Expenses",
                    fraction = state.expenseFraction,
                    amount = state.displayExpenseMinor,
                    color = money.expense,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.displayTotalMinor.formatSigned(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = if (state.displayTotalMinor < 0) money.expense else money.income
                    )
                }
            }

            FilterTabs(selected = tab, draft = draft, onSelect = { tab = it })
            Divider(color = MaterialTheme.colorScheme.outline)

            when (tab) {
                FilterTab.INCOME -> CategoryChecklist(
                    rows = state.incomeRows,
                    selectedIds = draft.incomeCategoryIds,
                    accent = money.income,
                    onToggle = { id ->
                        draft = draft.copy(incomeCategoryIds = draft.incomeCategoryIds.toggle(id))
                    },
                    onToggleAll = { selectAll ->
                        draft = draft.copy(
                            incomeCategoryIds = if (selectAll) {
                                state.incomeRows.map { it.category.id }.toSet()
                            } else {
                                emptySet()
                            }
                        )
                    }
                )

                FilterTab.EXPENSES -> CategoryChecklist(
                    rows = state.expenseRows,
                    selectedIds = draft.expenseCategoryIds,
                    accent = money.expense,
                    onToggle = { id ->
                        draft = draft.copy(expenseCategoryIds = draft.expenseCategoryIds.toggle(id))
                    },
                    onToggleAll = { selectAll ->
                        draft = draft.copy(
                            expenseCategoryIds = if (selectAll) {
                                state.expenseRows.map { it.category.id }.toSet()
                            } else {
                                emptySet()
                            }
                        )
                    }
                )

                FilterTab.ACCOUNT -> AccountChecklist(
                    rows = state.accountRows,
                    selectedIds = draft.accountIds,
                    onToggle = { id -> draft = draft.copy(accountIds = draft.accountIds.toggle(id)) },
                    onToggleAll = { selectAll ->
                        draft = draft.copy(
                            accountIds = if (selectAll) {
                                state.accountRows.map { it.account.id }.toSet()
                            } else {
                                emptySet()
                            }
                        )
                    }
                )
            }
        }
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

@Composable
private fun FilterBanner(
    draft: TransactionFilter,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (draft.isEmpty) {
                "Select items that you want to filter."
            } else {
                "${draft.selectionCount} selected"
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!draft.isEmpty) {
            Text(
                text = "Clear",
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Filter", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PercentRing(
    label: String,
    fraction: Float,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(92.dp)) {
                val stroke = 10.dp.toPx()
                val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
                val topLeft = Offset(
                    (size.width - arcSize.width) / 2f,
                    (size.height - arcSize.height) / 2f
                )
                drawArc(
                    color = color.copy(alpha = 0.28f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                if (fraction > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = "${(fraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = amount.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FilterTabs(
    selected: FilterTab,
    draft: TransactionFilter,
    onSelect: (FilterTab) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FilterTab.values().forEach { entry ->
            val isSelected = entry == selected
            val hasSelection = when (entry) {
                FilterTab.INCOME -> draft.incomeCategoryIds.isNotEmpty()
                FilterTab.EXPENSES -> draft.expenseCategoryIds.isNotEmpty()
                FilterTab.ACCOUNT -> draft.accountIds.isNotEmpty()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(entry) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.label.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (hasSelection) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }
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
private fun CategoryChecklist(
    rows: List<FilterCategoryRow>,
    selectedIds: Set<Long>,
    accent: Color,
    onToggle: (Long) -> Unit,
    onToggleAll: (Boolean) -> Unit
) {
    val allSelected = rows.isNotEmpty() && rows.all { it.category.id in selectedIds }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "all") {
            ChecklistRow(
                checked = allSelected,
                accent = accent,
                onCheckedChange = { onToggleAll(it) },
                leading = {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailing = {
                    Text(
                        text = "This month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline)
        }
        items(rows, key = { it.category.id }) { row ->
            val checked = row.category.id in selectedIds
            ChecklistRow(
                checked = checked,
                accent = accent,
                onCheckedChange = { onToggle(row.category.id) },
                leading = {
                    Text(text = row.category.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = row.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailing = {
                    if (row.amountMinor != 0L) {
                        Text(
                            text = row.amountMinor.formatMoney(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AccountChecklist(
    rows: List<FilterAccountRow>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onToggleAll: (Boolean) -> Unit
) {
    val money = LocalMoneyColors.current
    val allSelected = rows.isNotEmpty() && rows.all { it.account.id in selectedIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "all") {
            ChecklistRow(
                checked = allSelected,
                accent = MaterialTheme.colorScheme.primary,
                onCheckedChange = { onToggleAll(it) },
                leading = {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailing = {
                    Text(
                        text = "Income",
                        modifier = Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = money.income
                    )
                    Text(
                        text = "Expenses",
                        modifier = Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = money.expense
                    )
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline)
        }
        items(rows, key = { it.account.id }) { row ->
            ChecklistRow(
                checked = row.account.id in selectedIds,
                accent = MaterialTheme.colorScheme.primary,
                onCheckedChange = { onToggle(row.account.id) },
                leading = {
                    Text(
                        text = row.account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailing = {
                    Text(
                        text = row.incomeMinor.formatMoney(),
                        modifier = Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = if (row.incomeMinor == 0L) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            money.income
                        }
                    )
                    Text(
                        text = row.expenseMinor.formatMoney(),
                        modifier = Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = if (row.expenseMinor == 0L) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            money.expense
                        }
                    )
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ChecklistRow(
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) { leading() }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) { trailing() }
    }
}
