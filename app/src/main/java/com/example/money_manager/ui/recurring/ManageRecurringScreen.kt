package com.example.money_manager.ui.recurring

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.model.InstallmentPeriod
import com.example.money_manager.data.model.RecurrenceMode
import com.example.money_manager.data.model.RecurrenceRule
import com.example.money_manager.data.model.RepeatFrequency
import com.example.money_manager.ui.entry.ChoiceChip
import com.example.money_manager.ui.entry.toMinorUnits
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.entryDateLabel
import com.example.money_manager.util.formatMoney
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRecurringScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val money = LocalMoneyColors.current

    var editing by remember { mutableStateOf<RecurrenceRule?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurrenceRule?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Repeat setting", fontWeight = FontWeight.SemiBold) },
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
        }
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83D\uDD01", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No repeats or instalments yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Set one up with Rep/Inst. when adding an entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = rule }
                            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.template.note.ifBlank {
                                    rule.template.category?.name ?: "Transfer"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = viewModel.summaryOf(rule),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = viewModel.progressOf(rule),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rule.template.amountMinor.formatMoney(),
                            style = MaterialTheme.typography.titleMedium,
                            color = money.expense
                        )
                        IconButton(onClick = { pendingDelete = rule }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete schedule",
                                modifier = Modifier.size(22.dp),
                                tint = money.expense
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    editing?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { editing = null },
            onSave = { amountText, count, frequency, period ->
                viewModel.update(rule, amountText.toMinorUnits(), count, frequency, period)
                editing = null
            }
        )
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Stop this schedule?") },
            text = {
                Text(
                    "No further entries will be created. Entries already added are kept."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(rule)
                    pendingDelete = null
                }) {
                    Text("Stop", color = money.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditRuleDialog(
    rule: RecurrenceRule,
    onDismiss: () -> Unit,
    onSave: (String, Int, RepeatFrequency, InstallmentPeriod) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var amount by remember {
        mutableStateOf((rule.template.amountMinor / 100.0).let { "%.2f".format(it) })
    }
    var count by remember { mutableStateOf(rule.spec.count.toString()) }
    var frequency by remember { mutableStateOf(rule.spec.frequency) }
    var period by remember { mutableStateOf(rule.spec.period) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit schedule") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = count,
                    onValueChange = { input -> count = input.filter { it.isDigit() }.take(3) },
                    label = { Text("How many times") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (rule.spec.mode == RecurrenceMode.REPEAT) "Frequency" else "Period",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (rule.spec.mode == RecurrenceMode.REPEAT) {
                        RepeatFrequency.values().forEach { option ->
                            ChoiceChip(option.label, frequency == option, accent) {
                                frequency = option
                            }
                        }
                    } else {
                        InstallmentPeriod.values().forEach { option ->
                            ChoiceChip(option.label, period == option, accent) { period = option }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${rule.generatedCount} entries already added keep their dates and " +
                        "amounts. Changes apply from the next one onwards.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = count.toIntOrNull() != null,
                onClick = {
                    onSave(amount, count.toIntOrNull() ?: rule.spec.count, frequency, period)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
