package com.example.money_manager.ui.entry

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.money_manager.data.attachment.AttachmentStore
import com.example.money_manager.data.model.Account
import com.example.money_manager.data.model.Category
import com.example.money_manager.data.model.TransactionType
import com.example.money_manager.data.ofType
import com.example.money_manager.ui.theme.LocalMoneyColors
import com.example.money_manager.util.entryDateLabel
import com.example.money_manager.util.entryTimeLabel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

private enum class PendingSave { SAVE, CONTINUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    onClose: () -> Unit,
    onManageCategories: (TransactionType) -> Unit,
    viewModel: EntryViewModel
) {
    val form by viewModel.form.collectAsState()
    val sheet by viewModel.sheet.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val money = LocalMoneyColors.current
    val transferAccent = MaterialTheme.colorScheme.primary
    val accentFor: (TransactionType) -> Color = { type ->
        when (type) {
            TransactionType.INCOME -> money.income
            TransactionType.EXPENSE -> money.expense
            TransactionType.TRANSFER -> transferAccent
        }
    }
    val accent = accentFor(form.type)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<PendingSave?>(null) }

    fun toast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<File?>(null) }

    // Replacing or clearing the attachment drops the file it was pointing at.
    fun applyPhoto(name: String?) {
        val previous = viewModel.form.value.photoPath
        if (previous != null && previous != name) AttachmentStore.delete(context, previous)
        viewModel.setPhoto(name)
    }

    fun onPicked(uri: Uri?) {
        if (uri == null) return
        val stored = AttachmentStore.importPhoto(context, uri)
        if (stored == null) toast("Could not read that image") else applyPhoto(stored)
    }

    val cameraLauncher = rememberLauncherForActivityResult(CapturePhoto()) { saved ->
        val target = cameraTarget
        cameraTarget = null
        if (saved && target != null) applyPhoto(target.name) else target?.delete()
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> onPicked(uri) }
    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> onPicked(uri) }

    BackHandler(enabled = sheet != EntrySheet.NONE) { viewModel.closeSheet() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(form.type.label, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (form.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete transaction"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            PickerPanel(
                sheet = sheet,
                form = form,
                accounts = accounts,
                categories = categories,
                accent = accent,
                viewModel = viewModel,
                onManageCategories = { onManageCategories(form.type) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            TypeSelector(
                selected = form.type,
                accentFor = accentFor,
                onSelect = viewModel::setType
            )
            Spacer(Modifier.height(16.dp))

            FieldRow(label = "Date", accent = accent) {
                PickerValue(
                    value = form.dateTime.entryDateLabel(),
                    placeholder = "",
                    onClick = { showDatePicker = true }
                )
                Spacer(Modifier.width(16.dp))
                PickerValue(
                    value = form.dateTime.entryTimeLabel(),
                    placeholder = "",
                    onClick = { showTimePicker = true }
                )
                Spacer(Modifier.weight(1f))
                val recurrenceSet = form.recurrence != null
                Column(
                    modifier = Modifier
                        .clickable {
                            if (form.isEditing) {
                                toast("Repeat / instalment applies to new entries only")
                            } else {
                                viewModel.openSheet(EntrySheet.RECURRENCE)
                            }
                        }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (recurrenceSet) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = form.recurrence?.shortLabel() ?: "Rep/Inst.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (recurrenceSet) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (form.type == TransactionType.TRANSFER) {
                FieldRow(
                    label = "From",
                    accent = accent,
                    active = sheet == EntrySheet.ACCOUNT
                ) {
                    PickerValue(
                        value = form.account?.name,
                        placeholder = "Select account",
                        onClick = { viewModel.openSheet(EntrySheet.ACCOUNT) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = viewModel::swapAccounts, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.SwapVert,
                            contentDescription = "Swap accounts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FieldRow(
                    label = "To",
                    accent = accent,
                    active = sheet == EntrySheet.TO_ACCOUNT
                ) {
                    PickerValue(
                        value = form.toAccount?.name,
                        placeholder = "Select account",
                        onClick = { viewModel.openSheet(EntrySheet.TO_ACCOUNT) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FieldRow(
                    label = "Account",
                    accent = accent,
                    active = sheet == EntrySheet.ACCOUNT
                ) {
                    PickerValue(
                        value = form.account?.name,
                        placeholder = "Select account",
                        onClick = { viewModel.openSheet(EntrySheet.ACCOUNT) },
                        modifier = Modifier.weight(1f)
                    )
                }
                FieldRow(
                    label = "Category",
                    accent = accent,
                    active = sheet == EntrySheet.CATEGORY
                ) {
                    PickerValue(
                        value = form.category?.let { "${it.emoji}  ${it.name}" },
                        placeholder = "Select category",
                        onClick = { viewModel.openSheet(EntrySheet.CATEGORY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FieldRow(
                label = "Amount",
                accent = accent,
                active = sheet == EntrySheet.AMOUNT
            ) {
                PickerValue(
                    value = form.amountText.ifEmpty { null },
                    placeholder = "0",
                    onClick = { viewModel.openSheet(EntrySheet.AMOUNT) },
                    modifier = Modifier.weight(1f)
                )
                if (form.type == TransactionType.TRANSFER && !form.feeVisible) {
                    OutlinedButton(
                        onClick = viewModel::showFeeField,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = "Fees",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (form.type == TransactionType.TRANSFER && form.feeVisible) {
                FieldRow(
                    label = "Fees",
                    accent = accent,
                    active = sheet == EntrySheet.FEE
                ) {
                    PickerValue(
                        value = form.feeText.ifEmpty { null },
                        placeholder = "0",
                        onClick = { viewModel.openSheet(EntrySheet.FEE) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::removeFeeField, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove fee",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FieldRow(label = "Note", accent = accent) {
                InlineTextValue(
                    value = form.note,
                    placeholder = "",
                    onValueChange = viewModel::setNote,
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next,
                    onFocused = viewModel::closeSheet
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(
                modifier = Modifier.height(8.dp),
                thickness = 8.dp,
                color = MaterialTheme.colorScheme.background
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InlineTextValue(
                    value = form.description,
                    placeholder = "Description",
                    onValueChange = viewModel::setDescription,
                    modifier = Modifier.weight(1f),
                    onFocused = viewModel::closeSheet
                )
                IconButton(onClick = { showPhotoOptions = true }) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = "Attach photo",
                        tint = if (form.photoPath != null) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            form.photoPath?.let { photo ->
                AttachedPhoto(
                    photo = photo,
                    onView = { showPhotoPreview = true },
                    onRemove = { applyPhoto(null) }
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val error = viewModel.validationError()
                        when {
                            error != null -> toast(error)
                            form.recurrence != null -> pendingSave = PendingSave.SAVE
                            else -> {
                                viewModel.save()
                                onClose()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        val error = viewModel.validationError()
                        when {
                            error != null -> toast(error)
                            form.recurrence != null -> pendingSave = PendingSave.CONTINUE
                            else -> {
                                viewModel.saveAndContinue()
                                toast("Saved")
                            }
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.dateTime.atZone(zone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = form.dateTime.hour,
            initialMinute = form.dateTime.minute
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(LocalTime.of(timeState.hour, timeState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timeState)
            }
        }
    }

    pendingSave?.let { action ->
        val count = form.recurrence?.count ?: 1
        AlertDialog(
            onDismissRequest = { pendingSave = null },
            title = { Text("Schedule $count entries?") },
            text = { Text(form.recurrencePreview().orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    pendingSave = null
                    if (action == PendingSave.SAVE) {
                        viewModel.save()
                        onClose()
                    } else {
                        viewModel.saveAndContinue()
                        toast("Schedule created")
                    }
                }) { Text("Schedule") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSave = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    AttachmentStore.delete(context, form.photoPath)
                    viewModel.delete()
                    onClose()
                }) {
                    Text("Delete", color = money.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPhotoOptions) {
        PhotoSourceDialog(
            onDismiss = { showPhotoOptions = false },
            onCamera = {
                showPhotoOptions = false
                val target = AttachmentStore.newPhotoFile(context)
                cameraTarget = target
                val launched = runCatching {
                    cameraLauncher.launch(AttachmentStore.contentUriFor(context, target))
                }.isSuccess
                if (!launched) {
                    cameraTarget = null
                    target.delete()
                    toast("No camera app available")
                }
            },
            onGallery = {
                showPhotoOptions = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onFiles = {
                showPhotoOptions = false
                val launched = runCatching {
                    filesLauncher.launch(arrayOf("image/*"))
                }.isSuccess
                if (!launched) toast("No file picker available")
            }
        )
    }

    if (showPhotoPreview) {
        form.photoPath?.let { photo ->
            PhotoPreviewDialog(photo = photo, onDismiss = { showPhotoPreview = false })
        }
    }
}

/** Camera apps need an explicit grant to write into the FileProvider URI. */
private class CapturePhoto : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach photo") },
        text = {
            Column {
                PhotoSourceRow(Icons.Outlined.PhotoCamera, "Camera", onCamera)
                PhotoSourceRow(Icons.Outlined.PhotoLibrary, "Gallery", onGallery)
                PhotoSourceRow(Icons.Outlined.Folder, "Files", onFiles)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PhotoSourceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AttachedPhoto(photo: String, onView: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val thumbnail = rememberPhotoBitmap(photo, maxSize = 256)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onView),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Attached photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (AttachmentStore.exists(context, photo)) {
                "Photo attached"
            } else {
                "Photo missing"
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoPreviewDialog(photo: String, onDismiss: () -> Unit) {
    val full = rememberPhotoBitmap(photo, maxSize = 1600)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onDismiss)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (full != null) {
                Image(
                    bitmap = full,
                    contentDescription = "Attached photo",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Photo is no longer available",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Decodes the stored photo down to roughly [maxSize] pixels on its longest edge. */
@Composable
private fun rememberPhotoBitmap(photo: String, maxSize: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(photo, maxSize) {
        val file = AttachmentStore.file(context, photo)
        if (!file.exists()) return@remember null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSize) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        }.getOrNull()
    }
}

/** Always-visible choices for the field currently being filled. */
@Composable
private fun PickerPanel(
    sheet: EntrySheet,
    form: EntryFormState,
    accounts: List<Account>,
    categories: List<Category>,
    accent: Color,
    viewModel: EntryViewModel,
    onManageCategories: () -> Unit
) {
    if (sheet == EntrySheet.NONE) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        Divider(color = MaterialTheme.colorScheme.outline)
        when (sheet) {
            EntrySheet.ACCOUNT, EntrySheet.TO_ACCOUNT -> {
                val isFrom = sheet == EntrySheet.ACCOUNT
                SheetHeader(
                    title = if (form.type == TransactionType.TRANSFER) {
                        if (isFrom) "From account" else "To account"
                    } else {
                        "Accounts"
                    },
                    onClose = viewModel::closeSheet
                )
                AccountGrid(
                    accounts = accounts,
                    selectedId = if (isFrom) form.account?.id else form.toAccount?.id,
                    accent = accent,
                    onSelect = { account ->
                        if (isFrom) viewModel.setAccount(account) else viewModel.setToAccount(account)
                    }
                )
            }

            EntrySheet.CATEGORY -> {
                SheetHeader(
                    title = "Category",
                    onClose = viewModel::closeSheet,
                    actions = {
                        TextButton(onClick = onManageCategories) {
                            Text("Edit")
                        }
                    }
                )
                CategoryGrid(
                    categories = categories.ofType(form.type),
                    selectedId = form.category?.id,
                    accent = accent,
                    onSelect = viewModel::setCategory
                )
            }
            EntrySheet.AMOUNT, EntrySheet.FEE -> {
                val isFee = sheet == EntrySheet.FEE
                SheetHeader(
                    title = if (isFee) "Fees" else "Amount",
                    onClose = viewModel::closeSheet
                )
                KeypadValueBar(
                    label = if (isFee) "Fees" else "Amount",
                    value = if (isFee) form.feeText else form.amountText,
                    accent = accent
                )
                AmountKeypad(
                    accent = accent,
                    onKey = viewModel::onKeypadKey,
                    onBackspace = viewModel::onKeypadBackspace,
                    onClear = viewModel::onKeypadClear,
                    onEquals = viewModel::onKeypadEquals,
                    onDone = viewModel::onKeypadDone
                )            }

            EntrySheet.RECURRENCE -> {
                SheetHeader(
                    title = if (form.allowsInstallment) "Repeat / Installment" else "Repeat",
                    onClose = viewModel::closeSheet
                )
                RecurrencePanel(
                    spec = form.recurrence,
                    allowInstallment = form.allowsInstallment,
                    accent = accent,
                    preview = form.recurrencePreview(),
                    onModeChange = viewModel::setRecurrenceMode,
                    onFrequencyChange = viewModel::setRepeatFrequency,
                    onPeriodChange = viewModel::setInstallmentPeriod,
                    onCountChange = viewModel::setRecurrenceCount,
                    onDone = viewModel::closeSheet
                )
            }

            EntrySheet.NONE -> Unit
        }
    }
}
