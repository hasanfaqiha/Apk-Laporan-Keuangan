package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.AppLock

private const val PIN_LENGTH = 4

/**
 * Full-screen gate shown while the app lock is enabled and locked. The user can
 * unlock with the PIN keypad or, when enrolled, with strong biometrics
 * (fingerprint / face). The PIN is verified against the salted hash in
 * [AppLock]; success calls [onUnlocked].
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var wrongAttempts by remember { mutableIntStateOf(0) }
    var wrongMessage by remember { mutableStateOf<String?>(null) }

    val biometricsAvailable = remember { AppLock.biometricAvailable(context) }
    val activity = context as? FragmentActivity
    val biometricPrompt = remember(activity) {
        if (activity != null) {
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // User cancelled or no match: stay on the PIN pad.
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED
                        ) {
                            wrongMessage = errString.toString()
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        wrongMessage = null
                        wrongAttempts = 0
                        onUnlocked()
                    }
                }
            )
        } else {
            null
        }
    }

    fun submitPin(pin: String) {
        if (AppLock.verify(context, pin)) {
            wrongMessage = null
            wrongAttempts = 0
            onUnlocked()
        } else {
            wrongAttempts++
            wrongMessage = if (wrongAttempts >= 3) {
                "PIN salah. Coba lagi atau gunakan sidik jari."
            } else {
                "PIN salah, coba lagi."
            }
            entered = ""
        }
    }

    fun appendDigit(digit: String) {
        if (entered.length >= PIN_LENGTH) return
        wrongMessage = null
        val next = entered + digit
        entered = next
        if (next.length == PIN_LENGTH) submitPin(next)
    }

    fun launchBiometrics() {
        wrongMessage = null
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka KeuanganKu")
                .setSubtitle("Konfirmasi identitas untuk membuka aplikasi")
                .setNegativeButtonText("Batal")
                .build()
            biometricPrompt?.authenticate(promptInfo)
        } catch (e: Exception) {
            Toast.makeText(context, "Biometrik tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "KeuanganKu Terkunci",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Masukkan PIN untuk membuka aplikasi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(PIN_LENGTH) { index ->
                    val filled = index < entered.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            wrongMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                KeypadRow(keys = listOf("1", "2", "3"), onDigit = ::appendDigit)
                KeypadRow(keys = listOf("4", "5", "6"), onDigit = ::appendDigit)
                KeypadRow(keys = listOf("7", "8", "9"), onDigit = ::appendDigit)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Left slot: biometric shortcut when available
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = biometricsAvailable && biometricPrompt != null) {
                                launchBiometrics()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (biometricsAvailable && biometricPrompt != null) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Buka dengan sidik jari",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    KeypadKey("0") { appendDigit(it) }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = entered.isNotEmpty()) {
                                wrongMessage = null
                                entered = entered.dropLast(1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable { onClick(label) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun KeypadRow(keys: List<String>, onDigit: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        keys.forEach { key -> KeypadKey(label = key, onClick = onDigit) }
    }
}
