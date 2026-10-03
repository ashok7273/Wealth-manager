package com.example.money_manager.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.ui.common.AppBottomBar
import com.example.money_manager.ui.common.AppDestination
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigate: (AppDestination) -> Unit,
    onCategoryClick: (Long) -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val period by viewModel.period.collectAsState()
    val anchor by viewModel.anchor.collectAsState()
    val custom by viewModel.customRange.collectAsState()
    val type by viewModel.type.collectAsState()

    val range = remember(period, anchor, custom) { period.rangeFor(anchor, custom) }
    val state = remember(transactions, range, type) { buildStats(transactions, range, type) }

    val snackbarHostState = remember { SnackbarHostState() }
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                RangeAppBar(
                    period = period,
                    range = range,
                    onPrev = { viewModel.shiftRange(-1) },
                    onNext = { viewModel.shiftRange(1) },
                    onPeriodChange = viewModel::setPeriod,
                    onPickStart = { editingStart = true },
                    onPickEnd = { editingEnd = true }
                )
                TypeTabs(
                    selected = type,
                    incomeMinor = state.incomeMinor,
                    expenseMinor = state.expenseMinor,
                    onSelect = viewModel::setType
                )
            }
        },
        bottomBar = {
            AppBottomBar(selected = AppDestination.STATS, onSelect = onNavigate)
        }
    ) { innerPadding ->
        val swipeModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(period) {
                // Period mode has no next/previous window, so swiping is a no-op there.
                if (period == StatsPeriod.PERIOD) return@pointerInput
                var dragged = 0f
                val threshold = 48.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        if (dragged > threshold) viewModel.shiftRange(-1)
                        else if (dragged < -threshold) viewModel.shiftRange(1)
                    },
                    onHorizontalDrag = { _, delta -> dragged += delta }
                )
            }

        if (state.isEmpty) {
            Box(
                modifier = swipeModifier,
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83D\uDCCA", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Nothing to chart for this period",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = swipeModifier,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    CategoryDonut(
                        slices = state.slices,
                        centerLabel = if (type == TransactionType.INCOME) "Income" else "Expenses",
                        centerValue = state.totalMinor.formatMoney(),
                        centerCaption = if (state.entryCount == 1) {
                            "1 entry"
                        } else {
                            "${state.entryCount} entries"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 20.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
                items(state.slices, key = { it.category.id }) { slice ->
                    CategoryLegendRow(
                        slice = slice,
                        onClick = { onCategoryClick(slice.category.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (editingStart) {
        RangeDatePicker(
            initial = custom.start,
            onDismiss = { editingStart = false },
            onPicked = {
                viewModel.setCustomStart(it)
                editingStart = false
            }
        )
    }
    if (editingEnd) {
        RangeDatePicker(
            initial = custom.end,
            onDismiss = { editingEnd = false },
            onPicked = {
                viewModel.setCustomEnd(it)
                editingEnd = false
            }
        )
    }
}

@Composable
private fun RangeAppBar(
    period: StatsPeriod,
    range: DateRange,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPeriodChange: (StatsPeriod) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (period == StatsPeriod.PERIOD) {
            Spacer(Modifier.width(12.dp))
            DateChip(label = range.start.shortDate(), onClick = onPickStart)
            Text(
                text = "~",
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DateChip(label = range.end.shortDate(), onClick = onPickEnd)
        } else {
            IconButton(onClick = onPrev) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous period")
            }
            Text(
                text = period.title(range),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next period")
            }
        }
        Spacer(Modifier.weight(1f))
        PeriodMenu(selected = period, onSelect = onPeriodChange)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun DateChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Outlined.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PeriodMenu(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp)
                )
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = "Change period",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StatsPeriod.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TypeTabs(
    selected: TransactionType,
    incomeMinor: Long,
    expenseMinor: Long,
    onSelect: (TransactionType) -> Unit
) {
    val money = LocalMoneyColors.current
    Row(modifier = Modifier.fillMaxWidth()) {
        TypeTab(
            label = "Income",
            amount = incomeMinor,
            accent = money.income,
            selected = selected == TransactionType.INCOME,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(TransactionType.INCOME) }
        )
        TypeTab(
            label = "Expenses",
            amount = expenseMinor,
            accent = money.expense,
            selected = selected == TransactionType.EXPENSE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(TransactionType.EXPENSE) }
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun TypeTab(
    label: String,
    amount: Long,
    accent: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = amount.formatMoney(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) accent else Color.Transparent)
        )
    }
}
