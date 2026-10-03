package com.example.money_manager.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.data.ofType
import com.example.money_manager.ui.theme.LocalMoneyColors

private val RowHeight = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    type: TransactionType,
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = viewModel()
) {
    val all by viewModel.categories.collectAsState()
    val rows = remember(all, type) { all.ofType(type) }
    val money = LocalMoneyColors.current

    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (type == TransactionType.INCOME) {
                            "Income Category"
                        } else {
                            "Expenses Category"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add category")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        // Local copy so a drag can reorder live; committed to the repository on release.
        var order by remember(rows) { mutableStateOf(rows) }
        var dragIndex by remember { mutableStateOf(-1) }
        var dragOffset by remember { mutableStateOf(0f) }
        val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (order.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No categories yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            order.forEachIndexed { index, category ->
                val isDragging = index == dragIndex
                Column(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .background(
                                if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.background
                                }
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pendingDelete = category }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete ${category.name}",
                                modifier = Modifier.size(22.dp),
                                tint = money.expense
                            )
                        }
                        if (category.emoji.isNotBlank()) {
                            Text(text = category.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = category.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { editing = category }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit ${category.name}",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.DragHandle,
                            contentDescription = "Reorder ${category.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragIndex = index
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val current = dragIndex
                                            if (dragOffset > rowHeightPx / 2 &&
                                                current < order.lastIndex
                                            ) {
                                                order = order.toMutableList().apply {
                                                    add(current + 1, removeAt(current))
                                                }
                                                dragIndex = current + 1
                                                dragOffset -= rowHeightPx
                                            } else if (dragOffset < -rowHeightPx / 2 &&
                                                current > 0
                                            ) {
                                                order = order.toMutableList().apply {
                                                    add(current - 1, removeAt(current))
                                                }
                                                dragIndex = current - 1
                                                dragOffset += rowHeightPx
                                            }
                                        },
                                        onDragEnd = {
                                            viewModel.reorder(type, order)
                                            dragIndex = -1
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            dragIndex = -1
                                            dragOffset = 0f
                                        }
                                    )
                                }
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (adding) {
        CategoryDialog(
            title = "New category",
            initialName = "",
            onDismiss = { adding = false },
            onConfirm = { name ->
                viewModel.add(name, type)
                adding = false
            }
        )
    }

    editing?.let { category ->
        CategoryDialog(
            title = "Edit category",
            initialName = category.name,
            onDismiss = { editing = null },
            onConfirm = { name ->
                viewModel.rename(category, name)
                editing = null
            }
        )
    }

    pendingDelete?.let { category ->
        val affected = viewModel.transactionCountFor(category.id)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${category.name}?") },
            text = {
                Text(
                    if (affected == 0) {
                        "This cannot be undone."
                    } else {
                        "$affected transaction${if (affected == 1) "" else "s"} in this category " +
                            "will also be deleted. This cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category)
                    pendingDelete = null
                }) {
                    Text("Delete", color = money.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
