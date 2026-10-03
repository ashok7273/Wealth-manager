package com.example.money_manager.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.NEW_ACCOUNT_ID
import com.example.money_manager.ui.entry.toAmountText
import com.example.money_manager.ui.entry.toMinorUnits
import com.example.money_manager.util.CURRENCY_SYMBOL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onClose: () -> Unit,
    accountId: Long = NEW_ACCOUNT_ID,
    viewModel: AccountsViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val editing = remember(accountId) {
        viewModel.accounts.value.firstOrNull { it.id == accountId }
    }
    var group by remember(accountId) { mutableStateOf(editing?.group) }
    var name by remember(accountId) { mutableStateOf(editing?.name.orEmpty()) }
    var amount by remember(accountId) {
        mutableStateOf(
            editing?.openingBalanceMinor?.takeIf { it != 0L }?.toAmountText().orEmpty()
        )
    }
    var description by remember(accountId) { mutableStateOf(editing?.description.orEmpty()) }
    var settlementDay by remember(accountId) { mutableStateOf(editing?.settlementDay ?: 9) }
    var paymentDay by remember(accountId) { mutableStateOf(editing?.paymentDay ?: 1) }
    var showNameError by remember(accountId) { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editing == null) "Add Account" else "Edit Account",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (group == null) onClose() else group = null }) {
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
        val selected = group
        if (selected == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "header") {
                    Text(
                        text = "Account Group",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
                items(groups) { option ->
                    Text(
                        text = option.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { group = option }
                            .padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                AccountField(label = "Group") {
                    Text(
                        text = selected.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { group = null }
                            .padding(vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                AccountField(label = "Name", error = showNameError) {
                    AccountTextField(
                        value = name,
                        placeholder = "Account name",
                        onValueChange = {
                            name = it
                            showNameError = false
                        }
                    )
                }
                AccountField(label = "Amount") {
                    AccountTextField(
                        value = amount,
                        placeholder = "${CURRENCY_SYMBOL}0.00",
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { input ->
                            amount = input.filter { it.isDigit() || it == '.' }
                        }
                    )
                }
                AccountField(label = "Description") {
                    AccountTextField(
                        value = description,
                        placeholder = "",
                        onValueChange = { description = it }
                    )
                }

                if (selected.isCard) {
                    Spacer(Modifier.height(10.dp))
                    Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.background)
                    AccountField(label = "Settlement Date") {
                        DayPicker(day = settlementDay, onDayChange = { settlementDay = it })
                    }
                    AccountField(label = "Payment Date") {
                        DayPicker(day = paymentDay, onDayChange = { paymentDay = it })
                    }
                    CardCycleSummary(settlementDay = settlementDay, paymentDay = paymentDay)
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            showNameError = true
                        } else {
                            val settlement = settlementDay.takeIf { selected.isCard }
                            val payment = paymentDay.takeIf { selected.isCard }
                            if (editing == null) {
                                viewModel.addAccount(
                                    name = name,
                                    group = selected,
                                    openingBalanceMinor = amount.toMinorUnits() ?: 0L,
                                    description = description,
                                    settlementDay = settlement,
                                    paymentDay = payment
                                )
                            } else {
                                viewModel.updateAccount(
                                    account = editing,
                                    name = name,
                                    group = selected,
                                    openingBalanceMinor = amount.toMinorUnits() ?: 0L,
                                    description = description,
                                    settlementDay = settlement,
                                    paymentDay = payment
                                )
                            }
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DayPicker(day: Int, onDayChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = "Every $day",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            CardDayRange.forEach { option ->
                DropdownMenuItem(
                    text = { Text("Every $option") },
                    onClick = {
                        onDayChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CardCycleSummary(settlementDay: Int, paymentDay: Int) {
    val (payable, outstanding) = remember(settlementDay, paymentDay) {
        cardCycles(settlementDay, paymentDay)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        CycleRow(label = "Balance Payable", value = payable.label())
        Spacer(Modifier.height(10.dp))
        CycleRow(label = "Outst. Balance", value = outstanding.label())
    }
}

@Composable
private fun CycleRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AccountField(
    label: String,
    error: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) { content() }
            Divider(
                modifier = Modifier.padding(end = 16.dp),
                thickness = if (error) 2.dp else 1.dp,
                color = if (error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun AccountTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(modifier = Modifier.padding(vertical = 14.dp)) {
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}
