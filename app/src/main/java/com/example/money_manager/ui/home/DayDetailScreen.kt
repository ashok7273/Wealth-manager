package com.example.money_manager.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.util.dayTitle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    var date by remember(initialDate) { mutableStateOf(initialDate) }

    val group = remember(transactions, date) { buildDayGroup(transactions, date) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(date.dayTitle(), fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { date = date.minusDays(1) }) {
                            Icon(
                                Icons.Outlined.KeyboardArrowLeft,
                                contentDescription = "Previous day"
                            )
                        }
                        IconButton(onClick = { date = date.plusDays(1) }) {
                            Icon(
                                Icons.Outlined.KeyboardArrowRight,
                                contentDescription = "Next day"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                MonthSummaryBar(
                    MonthSummary(
                        incomeMinor = group.incomeMinor,
                        expenseMinor = group.expenseMinor
                    )
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(Unit) {
                var dragged = 0f
                val threshold = 48.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        if (dragged > threshold) date = date.minusDays(1)
                        else if (dragged < -threshold) date = date.plusDays(1)
                    },
                    onHorizontalDrag = { _, delta -> dragged += delta }
                )
            }

        if (group.items.isEmpty()) {
            Box(modifier = content, contentAlignment = Alignment.Center) {
                Text(
                    text = "No entries on this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = content.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(group.items, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        selected = false,
                        onClick = { onEditTransaction(transaction.id) },
                        onLongClick = { onEditTransaction(transaction.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
