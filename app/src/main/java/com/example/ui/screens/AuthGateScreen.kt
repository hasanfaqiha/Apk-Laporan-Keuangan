package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.FinanceViewModel

@Composable
fun AuthGateScreen(
    viewModel: FinanceViewModel,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp)
        ) {
            // App Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Text(
                    text = "Keuanganku Cloud",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Kelola keuangan pribadi Anda secara online, aman, dan tersinkronisasi di semua perangkat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Authentication Form Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Form Header Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { isRegisterMode = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Masuk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        TextButton(
                            onClick = { isRegisterMode = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Daftar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Alamat Email") },
                        placeholder = { Text("contoh@email.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_gate_email_input")
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min. 6 karakter)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_gate_password_input")
                    )

                    // Confirm Password Field (Only for Sign Up)
                    AnimatedVisibility(
                        visible = isRegisterMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Konfirmasi Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_gate_confirm_password_input")
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (email.trim().isEmpty() || password.trim().isEmpty()) {
                                Toast.makeText(context, "Email dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (password.length < 6) {
                                Toast.makeText(context, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isRegisterMode && password != confirmPassword) {
                                Toast.makeText(context, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isProcessing = true
                            val authInstance = com.google.firebase.auth.FirebaseAuth.getInstance()

                            if (isRegisterMode) {
                                authInstance.createUserWithEmailAndPassword(email.trim(), password.trim())
                                    .addOnSuccessListener {
                                        isProcessing = false
                                        Toast.makeText(context, "Akun berhasil dibuat & masuk!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isProcessing = false
                                        Toast.makeText(context, "Daftar gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                authInstance.signInWithEmailAndPassword(email.trim(), password.trim())
                                    .addOnSuccessListener {
                                        isProcessing = false
                                        Toast.makeText(context, "Selamat datang kembali!", Toast.LENGTH_SHORT).show()
                                        
                                        // Auto-sync
                                        viewModel.syncManager.performFullSync(
                                            onSuccess = {
                                                Toast.makeText(context, "Sinkronisasi cloud berhasil diselesaikan!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { syncError ->
                                                Toast.makeText(context, "Gagal mengunduh data cloud: ${syncError.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        isProcessing = false
                                        Toast.makeText(context, "Gagal masuk: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().testTag("auth_gate_submit_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Daftar Akun Baru" else "Masuk Sekarang",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Skip / Offline Mode Button
            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.outline
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("auth_gate_skip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan Mode Offline (Lokal Saja)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
