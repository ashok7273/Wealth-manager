package com.example.money_manager.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money_manager.util.formatCompactMoney
import com.example.money_manager.util.formatMoney
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CategoryDonut(
    slices: List<CategorySlice>,
    centerLabel: String,
    centerValue: String,
    centerCaption: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 188.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = size.minDimension * 0.20f
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )

            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = slice.fraction * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        // Small gap so neighbouring slices stay readable.
                        sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                }
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = centerValue,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = centerCaption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryLegendRow(
    slice: CategorySlice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PercentBadge(fraction = slice.fraction, color = slice.color)
        Spacer(Modifier.width(14.dp))
        Text(text = slice.category.emoji, fontSize = 17.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slice.category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (slice.entryCount == 1) "1 entry" else "${slice.entryCount} entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.amountMinor.formatMoney(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PercentBadge(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${(fraction * 100).roundToInt()}%",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A)
        )
    }
}

/** Six-month spend line for a single category. */
@Composable
fun MonthlyTrendChart(
    points: List<MonthPoint>,
    lineColor: Color,
    highlight: YearMonth?,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val max = points.maxOf { it.amountMinor }.coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp)
        ) {
            val topInset = 12f.dp.toPx()
            val bottomInset = 8f.dp.toPx()
            val usableHeight = size.height - topInset - bottomInset
            val slot = size.width / points.size

            fun offsetOf(index: Int): Offset {
                val fraction = points[index].amountMinor.toFloat() / max
                return Offset(
                    x = slot * (index + 0.5f),
                    y = topInset + usableHeight * (1f - fraction)
                )
            }

            drawLine(
                color = gridColor,
                start = Offset(0f, topInset + usableHeight),
                end = Offset(size.width, topInset + usableHeight),
                strokeWidth = 1f.dp.toPx()
            )

            val offsets = points.indices.map(::offsetOf)

            val area = Path().apply {
                moveTo(offsets.first().x, topInset + usableHeight)
                offsets.forEach { lineTo(it.x, it.y) }
                lineTo(offsets.last().x, topInset + usableHeight)
                close()
            }
            drawPath(area, color = lineColor.copy(alpha = 0.14f))

            val line = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = line,
                color = lineColor,
                style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
            )

            offsets.forEachIndexed { index, offset ->
                val isHighlight = points[index].yearMonth == highlight
                drawCircle(color = surfaceColor, radius = 5f.dp.toPx(), center = offset)
                drawCircle(
                    color = lineColor,
                    radius = if (isHighlight) 5f.dp.toPx() else 3.5f.dp.toPx(),
                    center = offset,
                    style = if (isHighlight) Fill else Stroke(width = 2f.dp.toPx())
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = point.amountMinor.formatCompactMoney(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (point.yearMonth == highlight) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1,
                        color = if (point.yearMonth == highlight) {
                            lineColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = point.yearMonth.month
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeDatePicker(    initial: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPicked(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
