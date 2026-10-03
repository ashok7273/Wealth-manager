package com.example.money_manager.ui.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.InstallmentPeriod
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceSpec
import com.example.money_manager.data.model.RepeatFrequency
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.util.OP_ADD
import com.example.money_manager.util.OP_DIVIDE
import com.example.money_manager.util.OP_MULTIPLY
import com.example.money_manager.util.OP_SUBTRACT
import com.example.money_manager.util.evaluateExpression
import com.example.money_manager.util.hasOperator
import com.example.money_manager.util.isCalculatorOperator

private val LabelWidth = 92.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(
    selected: TransactionType,
    accentFor: (TransactionType) -> Color,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER)
            .forEach { type ->
                val isSelected = type == selected
                val accent = accentFor(type)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, accent) else null,
                    onClick = { onSelect(type) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
    }
}

val TransactionType.label: String
    get() = when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

/** Label on the left, tappable value on the right, with an accent underline when [active]. */
@Composable
fun FieldRow(
    label: String,
    accent: Color,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(LabelWidth),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            Divider(
                modifier = Modifier.padding(end = 16.dp),
                thickness = if (active) 2.dp else 1.dp,
                color = if (active) accent else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun PickerValue(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = value ?: placeholder,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        color = if (value != null) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    )
}

@Composable
fun InlineTextValue(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onFocused: () -> Unit = {}
) {
    Box(modifier = modifier.padding(vertical = 14.dp)) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFocused() },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 20.dp, end = 8.dp)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        actions()
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun AccountGrid(
    accounts: List<Account>,
    selectedId: Long?,
    accent: Color,
    onSelect: (Account) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.heightIn(max = 320.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(accounts, key = { it.id }) { account ->
            GridCell(
                selected = account.id == selectedId,
                accent = accent,
                onClick = { onSelect(account) }
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    color = if (account.id == selectedId) accent else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedId: Long?,
    accent: Color,
    onSelect: (Category) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.heightIn(max = 360.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            GridCell(
                selected = category.id == selectedId,
                accent = accent,
                onClick = { onSelect(category) }
            ) {
                Text(text = category.emoji, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    maxLines = 1,
                    color = if (category.id == selectedId) accent else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GridCell(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(3.dp)
            .background(
                color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

private sealed interface KeyAction {
    data class Input(val char: Char) : KeyAction
    object DoubleZero : KeyAction
    object Backspace : KeyAction
    object Clear : KeyAction
    object Equals : KeyAction
    object Done : KeyAction
}

private data class KeypadKey(val label: String, val action: KeyAction)

private val KeypadRows = listOf(
    listOf(
        KeypadKey("7", KeyAction.Input('7')),
        KeypadKey("8", KeyAction.Input('8')),
        KeypadKey("9", KeyAction.Input('9')),
        KeypadKey(OP_DIVIDE.toString(), KeyAction.Input(OP_DIVIDE)),
        KeypadKey("", KeyAction.Backspace)
    ),
    listOf(
        KeypadKey("4", KeyAction.Input('4')),
        KeypadKey("5", KeyAction.Input('5')),
        KeypadKey("6", KeyAction.Input('6')),
        KeypadKey(OP_MULTIPLY.toString(), KeyAction.Input(OP_MULTIPLY)),
        KeypadKey("C", KeyAction.Clear)
    ),
    listOf(
        KeypadKey("1", KeyAction.Input('1')),
        KeypadKey("2", KeyAction.Input('2')),
        KeypadKey("3", KeyAction.Input('3')),
        KeypadKey(OP_SUBTRACT.toString(), KeyAction.Input(OP_SUBTRACT)),
        KeypadKey("=", KeyAction.Equals)
    ),
    listOf(
        KeypadKey("0", KeyAction.Input('0')),
        KeypadKey("00", KeyAction.DoubleZero),
        KeypadKey(".", KeyAction.Input('.')),
        KeypadKey(OP_ADD.toString(), KeyAction.Input(OP_ADD)),
        KeypadKey("Done", KeyAction.Done)
    )
)

@Composable
fun AmountKeypad(
    accent: Color,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        KeypadRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val isOperator = key.action is KeyAction.Input &&
                        key.action.char.isCalculatorOperator()
                    val background = when (key.action) {
                        KeyAction.Done -> accent
                        KeyAction.Equals -> accent.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    KeypadButton(
                        modifier = Modifier.weight(1f),
                        background = background,
                        onClick = {
                            when (val action = key.action) {
                                is KeyAction.Input -> onKey(action.char)
                                KeyAction.DoubleZero -> {
                                    onKey('0')
                                    onKey('0')
                                }

                                KeyAction.Backspace -> onBackspace()
                                KeyAction.Clear -> onClear()
                                KeyAction.Equals -> onEquals()
                                KeyAction.Done -> onDone()
                            }
                        }
                    ) {
                        when (key.action) {
                            KeyAction.Backspace -> Icon(
                                Icons.Outlined.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            KeyAction.Done -> Text(
                                text = key.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            else -> Text(
                                text = key.label,
                                fontSize = if (key.label.length > 1) 18.sp else 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    key.action == KeyAction.Equals || isOperator -> accent
                                    key.action == KeyAction.Clear -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(2.dp)
            .background(
                color = background ?: MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrencePanel(
    spec: RecurrenceSpec?,
    allowInstallment: Boolean,
    accent: Color,
    preview: String?,
    onModeChange: (RecurrenceMode?) -> Unit,
    onFrequencyChange: (RepeatFrequency) -> Unit,
    onPeriodChange: (InstallmentPeriod) -> Unit,
    onCountChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("None", spec == null, accent) { onModeChange(null) }
            ChoiceChip("Repeat", spec?.mode == RecurrenceMode.REPEAT, accent) {
                onModeChange(RecurrenceMode.REPEAT)
            }
            if (allowInstallment) {
                ChoiceChip("Installment", spec?.mode == RecurrenceMode.INSTALLMENT, accent) {
                    onModeChange(RecurrenceMode.INSTALLMENT)
                }
            }
        }

        if (spec != null) {
            SectionLabel(if (spec.mode == RecurrenceMode.REPEAT) "Frequency" else "Period")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (spec.mode == RecurrenceMode.REPEAT) {
                    RepeatFrequency.values().forEach { frequency ->
                        ChoiceChip(frequency.label, spec.frequency == frequency, accent) {
                            onFrequencyChange(frequency)
                        }
                    }
                } else {
                    InstallmentPeriod.values().forEach { period ->
                        ChoiceChip(period.label, spec.period == period, accent) {
                            onPeriodChange(period)
                        }
                    }
                }
            }

            SectionLabel(
                if (spec.mode == RecurrenceMode.REPEAT) {
                    "How many times"
                } else {
                    "Duration in ${spec.period.unitLabel}"
                }
            )
            CountStepper(count = spec.count, accent = accent, onCountChange = onCountChange)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 6, 12, 24, 36, 60).forEach { quick ->
                    ChoiceChip(quick.toString(), spec.count == quick, accent) { onCountChange(quick) }
                }
            }

            if (preview != null) {
                Text(
                    text = preview,
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(48.dp)
                .background(accent, RoundedCornerShape(10.dp))
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CountStepper(count: Int, accent: Color, onCountChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("\u2212") { onCountChange(count - 1) }
        Text(
            text = count.toString(),
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        StepperButton("+") { onCountChange(count + 1) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun KeypadValueBar(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = value.ifEmpty { "0" },
                textAlign = TextAlign.End,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = accent
            )
            if (value.hasOperator()) {
                val result = evaluateExpression(value)
                Text(
                    text = "= " + (result?.toPlainString() ?: "—"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
