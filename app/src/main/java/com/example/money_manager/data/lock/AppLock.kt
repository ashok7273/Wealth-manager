package com.example.money_manager.data.lock

import android.content.Context
import android.os.SystemClock
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

enum class LockDelay(val label: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0L),
    ONE_MINUTE("After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", 5 * 60_000L),
    FIFTEEN_MINUTES("After 15 minutes", 15 * 60_000L),
    ONE_HOUR("After 1 hour", 60 * 60_000L);

    companion object {
        fun fromMillis(millis: Long): LockDelay =
            values().firstOrNull { it.millis == millis } ?: IMMEDIATELY
    }
}

const val PASSCODE_LENGTH = 6

/**
 * Passcode and biometric gate. The PIN itself is never stored, only a salted hash,
 * and that lives in an encrypted preference file backed by the Android keystore.
 */
class AppLock(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        "app_lock",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isEnabled = MutableStateFlow(prefs.contains(KEY_HASH))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC, false))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _delay = MutableStateFlow(
        LockDelay.fromMillis(prefs.getLong(KEY_DELAY, 0L))
    )
    val delay: StateFlow<LockDelay> = _delay.asStateFlow()

    private val _isLocked = MutableStateFlow(_isEnabled.value)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundedAt: Long? = null

    fun setPasscode(passcode: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, hash(passcode, salt))
            .apply()
        _isEnabled.value = true
        _isLocked.value = false
    }

    fun verify(passcode: String): Boolean {
        val saltText = prefs.getString(KEY_SALT, null) ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        return hash(passcode, salt) == expected
    }

    fun disable() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).putBoolean(KEY_BIOMETRIC, false).apply()
        _isEnabled.value = false
        _biometricEnabled.value = false
        _isLocked.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _biometricEnabled.value = enabled
    }

    fun setDelay(delay: LockDelay) {
        prefs.edit().putLong(KEY_DELAY, delay.millis).apply()
        _delay.value = delay
    }

    fun unlock() {
        _isLocked.value = false
        backgroundedAt = null
    }

    fun onBackgrounded() {
        if (_isEnabled.value && !_isLocked.value) backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onForegrounded() {
        if (!_isEnabled.value) return
        val since = backgroundedAt ?: return
        if (SystemClock.elapsedRealtime() - since >= _delay.value.millis) _isLocked.value = true
    }

    private fun hash(passcode: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var value = salt + passcode.toByteArray()
        repeat(ITERATIONS) { value = digest.digest(value) }
        return Base64.encodeToString(value, Base64.NO_WRAP)
    }

    private companion object {
        const val KEY_HASH = "passcode_hash"
        const val KEY_SALT = "passcode_salt"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_DELAY = "lock_delay"
        const val ITERATIONS = 20_000
    }
}
