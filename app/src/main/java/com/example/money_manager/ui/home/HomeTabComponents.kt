package com.example.money_manager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatMoney
import com.example.money_manager.util.formatSigned
import com.example.money_manager.util.formatSignedWhole
import com.example.money_manager.util.formatWholeAmount
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------- Calendar ----------

@Composable
fun CalendarGrid(
    cells: List<CalendarCell>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val money = LocalMoneyColors.current
    val outline = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            WeekdayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (index) {
                        0 -> money.expense
                        6 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        Divider(color = outline)

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        isToday = cell.date == today,
                        onClick = { onDayClick(cell.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Divider(color = outline)
        }
    }
}

private val WeekdayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val money = LocalMoneyColors.current
    val dim = if (cell.inMonth) 1f else 0.38f

    Column(
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = if (isToday) {
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            } else {
                Modifier.size(20.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = dim)
                }
            )
        }
        Spacer(Modifier.height(2.dp))
        if (cell.incomeMinor != 0L) {
            CalendarAmount(cell.incomeMinor, money.income.copy(alpha = dim))
        }
        if (cell.expenseMinor != 0L) {
            CalendarAmount(cell.expenseMinor, money.expense.copy(alpha = dim))
        }
        if (cell.hasBoth) {
            CalendarAmount(
                cell.netMinor,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                signed = true
            )
        }
    }
}

@Composable
private fun CalendarAmount(amountMinor: Long, color: Color, signed: Boolean = false) {
    Text(
        text = if (signed) amountMinor.formatSignedWhole() else amountMinor.formatWholeAmount(),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        color = color
    )
}

// ---------- Monthly ----------

@Composable
fun MonthRowItem(
    row: MonthRow,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val money = LocalMoneyColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (row.hasData) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.weight(1f))
            AmountTriple(
                incomeMinor = row.incomeMinor,
                expenseMinor = row.expenseMinor,
                totalMinor = row.totalMinor,
                incomeColor = money.income,
                expenseColor = money.expense
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                row.weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 40.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${week.start.dayMonth()} ~ ${week.end.dayMonth()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        AmountTriple(
                            incomeMinor = week.incomeMinor,
                            expenseMinor = week.expenseMinor,
                            totalMinor = week.totalMinor,
                            incomeColor = money.income,
                            expenseColor = money.expense,
                            compact = true
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun AmountTriple(
    incomeMinor: Long,
    expenseMinor: Long,
    totalMinor: Long,
    incomeColor: Color,
    expenseColor: Color,
    compact: Boolean = false
) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = incomeMinor.formatMoney(),
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (incomeMinor == 0L) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    incomeColor
                }
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = expenseMinor.formatMoney(),
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (expenseMinor == 0L) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    expenseColor
                }
            )
        }
        Text(
            text = totalMinor.formatSigned(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun LocalDate.dayMonth(): String =
    "%02d.%02d".format(dayOfMonth, monthValue)

// ---------- Total ----------

@Composable
fun TotalsContent(
    state: TotalsUiState,
    modifier: Modifier = Modifier
) {
    val money = LocalMoneyColors.current

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.yearMonth.atDay(1).shortDate()} ~ " +
                    state.yearMonth.atEndOfMonth().shortDate(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        TotalsCard {
            TotalsRow(
                label = "Compared expenses",
                caption = "vs last month",
                value = when {
                    state.changePercent == null -> "—"
                    state.changePercent > 0 -> "\u25B2 ${state.changePercent}%"
                    state.changePercent < 0 -> "\u25BC ${-state.changePercent}%"
                    else -> "0%"
                },
                valueColor = when {
                    state.changePercent == null || state.changePercent == 0 ->
                        MaterialTheme.colorScheme.onSurfaceVariant

                    state.changePercent > 0 -> money.expense
                    else -> money.income
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        TotalsCard {
            if (state.accounts.isEmpty()) {
                TotalsRow(
                    label = "No activity this month",
                    caption = null,
                    value = "",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.accounts.forEach { account ->
                TotalsRow(
                    label = "Expenses",
                    caption = account.accountName,
                    value = account.expenseMinor.formatMoney(),
                    valueColor = if (account.expenseMinor == 0L) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        money.expense
                    }
                )
            }
            state.accounts.filter { it.incomeMinor != 0L }.forEach { account ->
                TotalsRow(
                    label = "Income",
                    caption = account.accountName,
                    value = account.incomeMinor.formatMoney(),
                    valueColor = money.income
                )
            }
            TotalsRow(
                label = "Transfers",
                caption = null,
                value = state.transferMinor.formatMoney(),
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TotalsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun TotalsRow(
    label: String,
    caption: String?,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (caption != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "($caption)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}

private fun LocalDate.shortDate(): String =
    "%02d.%02d.%02d".format(dayOfMonth, monthValue, year % 100)
