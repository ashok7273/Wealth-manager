package com.example.money_manager.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Check
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
import com.example.money_manager.data.model.Transaction
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.formatAmount
import com.example.money_manager.util.formatSigned
import com.example.money_manager.util.monthYearLabel
import com.example.money_manager.util.weekdayShort

@Composable
fun MonthSummaryBar(summary: MonthSummary, modifier: Modifier = Modifier) {
    val money = LocalMoneyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        SummaryCell("Income", summary.incomeMinor.formatAmount(), money.income, Modifier.weight(1f))
        SummaryCell("Expenses", summary.expenseMinor.formatAmount(), money.expense, Modifier.weight(1f))
        SummaryCell(
            "Total",
            summary.totalMinor.formatSigned(),
            if (summary.totalMinor < 0) money.expense else money.income,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DayHeaderRow(group: DayGroup, modifier: Modifier = Modifier) {
    val money = LocalMoneyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.date.dayOfMonth.toString().padStart(2, '0'),
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
                text = group.date.weekdayShort(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = group.date.monthYearLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = group.incomeMinor.formatAmount(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (group.incomeMinor > 0) money.income else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = group.expenseMinor.formatAmount(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (group.expenseMinor > 0) money.expense else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val money = LocalMoneyColors.current
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> money.income
        TransactionType.EXPENSE -> money.expense
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(text = transaction.category?.emoji ?: "\u21C4", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { transaction.category?.name ?: "Transfer" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val subtitle = if (transaction.type == TransactionType.TRANSFER) {
                "${transaction.account.name}  \u2192  ${transaction.toAccount?.name.orEmpty()}"
            } else {
                "${transaction.category?.name.orEmpty()}  •  ${transaction.account.name}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = transaction.amountMinor.formatAmount(),
            style = MaterialTheme.typography.titleMedium,
            color = amountColor
        )
    }
}

@Composable
fun EmptyMonthState(isFiltered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isFiltered) "\uD83D\uDD0D" else "\uD83E\uDDFE", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isFiltered) "No matching transactions" else "No transactions this month",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isFiltered) {
                "Try a different note or amount."
            } else {
                "Tap + to add your first entry."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
