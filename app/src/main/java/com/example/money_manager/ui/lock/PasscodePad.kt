package com.example.money_manager.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.money_manager.data.lock.PASSCODE_LENGTH

/**
 * Six-digit keypad. [onFilled] fires once the last digit is entered and the field resets.
 */
@Composable
fun PasscodePad(
    title: String,
    subtitle: String?,
    errorMessage: String?,
    showBiometric: Boolean = false,
    onBiometric: () -> Unit = {},
    onCancel: (() -> Unit)? = null,
    onFilled: (String) -> Unit
) {
    var entry by remember { mutableStateOf("") }

    LaunchedEffect(entry) {
        if (entry.length == PASSCODE_LENGTH) {
            val value = entry
            entry = ""
            onFilled(value)
        }
    }
    LaunchedEffect(errorMessage) { if (errorMessage != null) entry = "" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(PASSCODE_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < entry.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.weight(1f))
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    PadKey(modifier = Modifier.weight(1f), onClick = { entry += digit }) {
                        Text(
                            text = digit,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            PadKey(
                modifier = Modifier.weight(1f),
                enabled = showBiometric,
                onClick = onBiometric
            ) {
                if (showBiometric) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = "Use fingerprint",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            PadKey(modifier = Modifier.weight(1f), onClick = { entry += "0" }) {
                Text(
                    text = "0",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            PadKey(
                modifier = Modifier.weight(1f),
                onClick = { entry = entry.dropLast(1) }
            ) {
                Icon(
                    Icons.Outlined.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                Text("Cancel")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PadKey(
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

@Composable
fun rememberBiometricAvailability(): Boolean {
    val context = LocalContext.current
    return remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
}

fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailed: () -> Unit = {}
) {
    val prompt = BiometricPrompt(
        activity,
        androidx.core.content.ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) = onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailed()
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Money Manager")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Use passcode")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    )
}
