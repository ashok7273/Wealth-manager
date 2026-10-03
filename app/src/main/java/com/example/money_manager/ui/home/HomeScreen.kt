package com.example.money_manager.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.ui.common.AppBottomBar
import com.example.money_manager.ui.common.AppDestination
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.title
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpenFilter: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val transactions = remember(allTransactions, filter) {
        if (filter.isEmpty) allTransactions else allTransactions.filter(filter::matches)
    }
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()

    val pagerState = rememberPagerState(
        initialPage = HomeViewModel.pageOf(selectedMonth),
        pageCount = { HomeViewModel.PAGER_MONTH_COUNT }
    )
    val yearPagerState = rememberPagerState(
        initialPage = HomeViewModel.yearPageOf(selectedMonth.year),
        pageCount = { HomeViewModel.PAGER_YEAR_COUNT }
    )
    val yearPaged = selectedTab.isYearPaged
    val expandedMonths = remember { mutableStateListOf<Int>() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun comingSoon(feature: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("$feature — coming soon")
        }
    }

    // Pager swipe -> selected month/year
    LaunchedEffect(pagerState, yearPaged) {
        if (yearPaged) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.selectMonth(HomeViewModel.monthOf(page))
        }
    }
    LaunchedEffect(yearPagerState, yearPaged) {
        if (!yearPaged) return@LaunchedEffect
        snapshotFlow { yearPagerState.currentPage }.collect { page ->
            viewModel.selectYear(HomeViewModel.yearOf(page))
        }
    }
    // Arrow taps and tab switches -> pager
    LaunchedEffect(selectedMonth, yearPaged) {
        if (yearPaged) {
            val target = HomeViewModel.yearPageOf(selectedMonth.year)
            if (target != yearPagerState.currentPage) yearPagerState.animateScrollToPage(target)
        } else {
            val target = HomeViewModel.pageOf(selectedMonth)
            if (target != pagerState.currentPage) pagerState.animateScrollToPage(target)
        }
    }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                when {
                    selectionMode -> SelectionAppBar(
                        count = selectedIds.size,
                        onClear = viewModel::clearSelection,
                        onDelete = { showDeleteConfirm = true }
                    )

                    searchActive -> SearchBar(
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        onClose = viewModel::closeSearch
                    )

                    else -> MonthAppBar(
                        title = if (yearPaged) {
                            selectedMonth.year.toString()
                        } else {
                            selectedMonth.title()
                        },
                        onPrev = {
                            if (yearPaged) {
                                viewModel.selectYear(selectedMonth.year - 1)
                            } else {
                                viewModel.selectMonth(selectedMonth.minusMonths(1))
                            }
                        },
                        onNext = {
                            if (yearPaged) {
                                viewModel.selectYear(selectedMonth.year + 1)
                            } else {
                                viewModel.selectMonth(selectedMonth.plusMonths(1))
                            }
                        },
                        onTitleClick = { comingSoon("Month picker") },
                        onSearch = viewModel::openSearch,
                        onFilter = onOpenFilter,
                        filterActive = !filter.isEmpty
                    )
                }
                HomeTabRow(
                    selected = selectedTab,
                    onSelect = viewModel::selectTab
                )
                if (!filter.isEmpty && !selectionMode && !searchActive) {
                    ActiveFilterBar(
                        count = filter.selectionCount,
                        onClear = viewModel::clearFilter
                    )
                }
            }
        },
        bottomBar = {
            AppBottomBar(selected = AppDestination.TRANSACTIONS, onSelect = onNavigate)
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { innerPadding ->
        val pagerModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        if (yearPaged) {
            HorizontalPager(
                state = yearPagerState,
                modifier = pagerModifier,
                userScrollEnabled = !searchActive,
                key = { it }
            ) { page ->
                val year = remember(page) { HomeViewModel.yearOf(page) }
                val state = remember(transactions, year, query) {
                    buildYearState(transactions, year, query)
                }
                YearPage(
                    state = state,
                    expandedMonths = expandedMonths,
                    onToggleMonth = { monthValue ->
                        if (!expandedMonths.remove(monthValue)) expandedMonths.add(monthValue)
                    }
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = pagerModifier,
                userScrollEnabled = !searchActive && !selectionMode,
                key = { it }
            ) { page ->
                val month = remember(page) { HomeViewModel.monthOf(page) }
                when (selectedTab) {
                    HomeTab.DAILY -> {
                        val state = remember(transactions, month, query) {
                            buildMonthState(transactions, month, query)
                        }
                        MonthPage(
                            state = state,
                            selectedIds = selectedIds,
                            onTransactionClick = { id ->
                                if (selectionMode) {
                                    viewModel.toggleSelection(id)
                                } else {
                                    onEditTransaction(id)
                                }
                            },
                            onTransactionLongClick = viewModel::toggleSelection
                        )
                    }

                    HomeTab.CALENDAR -> {
                        val state = remember(transactions, month, query) {
                            buildMonthState(transactions, month, query)
                        }
                        val cells = remember(transactions, month, query) {
                            buildCalendarCells(transactions, month, query)
                        }
                        CalendarPage(summary = state.summary, cells = cells, onDayClick = onDayClick)
                    }

                    HomeTab.TOTAL -> {
                        val state = remember(transactions, month, query) {
                            buildTotalsState(transactions, month, query)
                        }
                        TotalPage(state = state)
                    }

                    HomeTab.MONTHLY -> Unit
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (count == 1) "Delete this transaction?" else "Delete $count transactions?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelected()
                }) {
                    Text("Delete", color = LocalMoneyColors.current.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActiveFilterBar(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtered by $count item${if (count == 1) "" else "s"}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        TextButton(onClick = onClear) {
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SelectionAppBar(count: Int, onClear: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "Exit selection")
        }
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Delete selected",
                tint = LocalMoneyColors.current.expense
            )
        }
    }
}

@Composable
private fun CalendarPage(
    summary: MonthSummary,
    cells: List<CalendarCell>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        MonthSummaryBar(summary)
        Divider(color = MaterialTheme.colorScheme.outline)
        CalendarGrid(cells = cells, today = today, onDayClick = onDayClick)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun TotalPage(state: TotalsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        MonthSummaryBar(state.summary)
        Divider(color = MaterialTheme.colorScheme.outline)
        TotalsContent(state)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun YearPage(
    state: YearUiState,
    expandedMonths: List<Int>,
    onToggleMonth: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MonthSummaryBar(state.summary)
        Divider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(state.months, key = { it.yearMonth.monthValue }) { row ->
                MonthRowItem(
                    row = row,
                    expanded = row.yearMonth.monthValue in expandedMonths,
                    onToggle = { onToggleMonth(row.yearMonth.monthValue) }
                )
            }
        }
    }
}

@Composable
private fun MonthPage(
    state: MonthUiState,
    selectedIds: Set<Long>,
    onTransactionClick: (Long) -> Unit,
    onTransactionLongClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MonthSummaryBar(state.summary)
        Divider(color = MaterialTheme.colorScheme.outline)
        if (state.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyMonthState(isFiltered = state.isFiltered)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                state.days.forEach { group ->
                    item(key = "header-${group.date}") {
                        DayHeaderRow(group)
                    }
                    items(group.items, key = { it.id }) { txn ->
                        TransactionRow(
                            transaction = txn,
                            selected = txn.id in selectedIds,
                            onClick = { onTransactionClick(txn.id) },
                            onLongClick = { onTransactionLongClick(txn.id) }
                        )
                    }
                    item(key = "gap-${group.date}") {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthAppBar(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    filterActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(onClick = onTitleClick)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, contentDescription = "Search")
        }
        IconButton(onClick = onFilter) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = "Filter",
                tint = if (filterActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text("Search note or amount") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Close search")
        }
    }
}

@Composable
private fun HomeTabRow(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    TabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { Divider(color = MaterialTheme.colorScheme.outline) }
    ) {
        HomeTab.values().forEach { tab ->
            val isSelected = tab == selected
            // Slot overload avoids Tab's built-in 16dp horizontal text padding.
            Tab(
                selected = isSelected,
                onClick = { onSelect(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = tab.label,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 14.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
