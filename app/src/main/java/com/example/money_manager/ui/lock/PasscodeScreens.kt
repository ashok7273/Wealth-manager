package com.example.money_manager.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.money_manager.data.ServiceLocator
import com.example.money_manager.data.lock.LockDelay

private enum class SetupStage { ENTER, CONFIRM }

private enum class ProtectedAction { DISABLE, CHANGE }

/** Full-screen gate shown when the app is locked. */
@Composable
fun LockScreen() {
    val lock = ServiceLocator.appLock
    val biometricEnabled by lock.biometricEnabled.collectAsState()
    val biometricAvailable = rememberBiometricAvailability()
    val activity = LocalContext.current as? FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }

    val canUseBiometric = biometricEnabled && biometricAvailable && activity != null

    fun promptBiometric() {
        activity?.let { showBiometricPrompt(it, onSuccess = lock::unlock) }
    }

    androidx.compose.runtime.LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) promptBiometric()
    }

    PasscodePad(
        title = "Enter passcode",
        subtitle = null,
        errorMessage = error,
        showBiometric = canUseBiometric,
        onBiometric = ::promptBiometric,
        onFilled = { entered ->
            if (lock.verify(entered)) {
                error = null
                lock.unlock()
            } else {
                error = "Incorrect passcode"
            }
        }
    )
}

/** Six-digit set-up: enter, then re-enter to confirm. */
@Composable
fun SetPasscodeScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val lock = ServiceLocator.appLock
    var stage by remember { mutableStateOf(SetupStage.ENTER) }
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PasscodePad(
        title = if (stage == SetupStage.ENTER) "Set a passcode" else "Re-enter passcode",
        subtitle = "6 digits",
        errorMessage = error,
        onCancel = onCancel,
        onFilled = { entered ->
            if (stage == SetupStage.ENTER) {
                first = entered
                error = null
                stage = SetupStage.CONFIRM
            } else if (entered == first) {
                lock.setPasscode(entered)
                onDone()
            } else {
                error = "Passcodes did not match"
                stage = SetupStage.ENTER
                first = ""
            }
        }
    )
}

/** Verifies the current passcode before showing passcode settings. */
@Composable
fun VerifyPasscodeScreen(onVerified: () -> Unit, onCancel: () -> Unit) {
    val lock = ServiceLocator.appLock
    var error by remember { mutableStateOf<String?>(null) }

    PasscodePad(
        title = "Enter passcode",
        subtitle = null,
        errorMessage = error,
        onCancel = onCancel,
        onFilled = { entered ->
            if (lock.verify(entered)) onVerified() else error = "Incorrect passcode"
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasscodeSettingsScreen(
    onBack: () -> Unit,
    onChangePasscode: () -> Unit,
    onTurnedOff: () -> Unit
) {
    val lock = ServiceLocator.appLock
    val biometricEnabled by lock.biometricEnabled.collectAsState()
    val delay by lock.delay.collectAsState()
    val biometricAvailable = rememberBiometricAvailability()
    var showDelayPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<ProtectedAction?>(null) }

    // Turning the passcode off or changing it needs the current one first.
    pendingAction?.let { action ->
        VerifyPasscodeScreen(
            onVerified = {
                pendingAction = null
                when (action) {
                    ProtectedAction.DISABLE -> {
                        lock.disable()
                        onTurnedOff()
                    }

                    ProtectedAction.CHANGE -> onChangePasscode()
                }
            },
            onCancel = { pendingAction = null }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Passcode", fontWeight = FontWeight.SemiBold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SettingRow(label = "Turn off Passcode", onClick = {
                pendingAction = ProtectedAction.DISABLE
            })
            Divider(color = MaterialTheme.colorScheme.outline)

            SettingRow(label = "Change Passcode", onClick = {
                pendingAction = ProtectedAction.CHANGE
            })
            Divider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometrics",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!biometricAvailable) {
                        Text(
                            text = "No fingerprint or face enrolled on this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = lock::setBiometricEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Request Passcode",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDelayPicker = true }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request Passcode",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = delay.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline)
        }
    }

    if (showDelayPicker) {
        Dialog(onDismissRequest = { showDelayPicker = false }) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Request Passcode",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showDelayPicker = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline)
                LockDelay.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lock.setDelay(option)
                                showDelayPicker = false
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == delay) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (option == delay) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
