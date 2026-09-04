package com.example.data

import android.content.Context
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * App-lock (PIN + biometrics) support.
 *
 * The PIN is never stored in plain text: only a salted SHA-256 hash lives in
 * SharedPreferences (same `finance_prefs` file already excluded from Android
 * Auto Backup via backup_rules.xml / data_extraction_rules.xml).
 *
 * [enabled] / [locked] are process-wide state flows so the lock gate overlay in
 * MainActivity reacts immediately when the user enables or disables the lock in
 * Settings.
 */
object AppLock {
    private const val PREFS = "finance_prefs"
    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_PIN_SALT = "app_lock_pin_salt"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True when a PIN has been configured (used once per process at startup). */
    fun isPinConfigured(context: Context): Boolean {
        val p = prefs(context)
        return !p.getString(KEY_PIN_HASH, "").isNullOrBlank()
    }

    /** Called once when the app starts: enables the lock when a PIN exists. */
    fun init(context: Context) {
        val configured = isPinConfigured(context)
        _enabled.value = configured
        // Cold start with a lock configured => locked until the user unlocks.
        _locked.value = configured
    }

    /** Enables the lock with a new PIN. */
    fun enable(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.toHex()
        prefs(context).edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hash(saltHex, pin))
            .apply()
        _enabled.value = true
    }

    /** Disables the lock entirely. */
    fun disable(context: Context) {
        prefs(context).edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
        _enabled.value = false
        _locked.value = false
    }

    /** Checks a PIN against the stored hash. */
    fun verify(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(KEY_PIN_SALT, "") ?: ""
        val stored = p.getString(KEY_PIN_HASH, "") ?: ""
        return salt.isNotEmpty() && stored.isNotEmpty() && stored == hash(salt, pin)
    }

    /** Locks the app (used when the activity goes to the background). */
    fun lockNow() {
        if (_enabled.value) _locked.value = true
    }

    /** Unlocks the app after a successful PIN / biometric check. */
    fun unlock() {
        _locked.value = false
    }

    /** True when strong biometrics are enrolled on this device. */
    fun biometricAvailable(context: Context): Boolean {
        return try {
            val result = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            result == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    private fun hash(saltHex: String, pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((saltHex + pin).toByteArray())
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
