package com.example.money_manager.ui.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatCompactMoney
import com.example.money_manager.util.formatSigned
import com.example.money_manager.util.title
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val month by viewModel.statsMonth.collectAsState()

    val points = remember(accounts, transactions, month) {
        buildAccountStats(accounts, transactions, month)
    }
    val money = LocalMoneyColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Total Stats", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftStatsMonth(-1) }) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = month.title(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.shiftStatsMonth(1) }) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next month")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    var dragged = 0f
                    val threshold = 48.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged > threshold) viewModel.shiftStatsMonth(-1)
                            else if (dragged < -threshold) viewModel.shiftStatsMonth(1)
                        },
                        onHorizontalDrag = { _, delta -> dragged += delta }
                    )
                }
        ) {
            val balance = points.lastOrNull()?.balanceMinor ?: 0L
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = balance.formatSigned(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) money.expense else MaterialTheme.colorScheme.onSurface
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            BalanceLineChart(
                points = points,
                lineColor = if (balance < 0) money.expense else money.income
            )
            Divider(color = MaterialTheme.colorScheme.outline)

            IncomeExpenseBarChart(
                points = points,
                incomeColor = money.income,
                expenseColor = money.expense
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BalanceLineChart(points: List<AccountStatsPoint>, lineColor: Color) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val max = points.maxOf { it.balanceMinor }
    val min = points.minOf { it.balanceMinor }
    val span = (max - min).coerceAtLeast(1L)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp)
        ) {
            val top = 16f.dp.toPx()
            val bottom = 12f.dp.toPx()
            val usable = size.height - top - bottom
            val slot = size.width / points.size

            repeat(3) { index ->
                val y = top + usable * index / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f.dp.toPx()
                )
            }

            val offsets = points.mapIndexed { index, point ->
                val fraction = (point.balanceMinor - min).toFloat() / span
                Offset(slot * (index + 0.5f), top + usable * (1f - fraction))
            }

            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
            )
            offsets.forEach { offset ->
                drawCircle(color = surface, radius = 5f.dp.toPx(), center = offset)
                drawCircle(color = lineColor, radius = 4f.dp.toPx(), center = offset)
            }
        }
        MonthLabels(points) { point ->
            Text(
                text = point.balanceMinor.formatCompactMoney(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomeExpenseBarChart(
    points: List<AccountStatsPoint>,
    incomeColor: Color,
    expenseColor: Color
) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outline
    val max = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp)
        ) {
            val top = 16f.dp.toPx()
            val bottom = 12f.dp.toPx()
            val usable = size.height - top - bottom
            val slot = size.width / points.size
            val barWidth = slot * 0.22f

            repeat(4) { index ->
                val y = top + usable * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f.dp.toPx()
                )
            }

            points.forEachIndexed { index, point ->
                val centre = slot * (index + 0.5f)
                listOf(
                    point.incomeMinor to incomeColor,
                    point.expenseMinor to expenseColor
                ).forEachIndexed { barIndex, (value, color) ->
                    if (value > 0L) {
                        val barHeight = usable * (value.toFloat() / max)
                        val left = centre + (barIndex - 1) * barWidth - barWidth * 0.1f
                        drawRect(
                            color = color,
                            topLeft = Offset(left, top + usable - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }
            }
        }
        MonthLabels(points) { point ->
            Text(
                text = point.incomeMinor.formatCompactMoney(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = incomeColor
            )
            Text(
                text = point.expenseMinor.formatCompactMoney(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = expenseColor
            )
        }
    }
}

@Composable
private fun MonthLabels(
    points: List<AccountStatsPoint>,
    values: @Composable (AccountStatsPoint) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        points.forEach { point ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = point.month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                values(point)
            }
        }
    }
}
