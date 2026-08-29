package com.example.dailytrack_mobile.presentation.screens.lock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.dailytrack_mobile.data.local.security.AppLockManager
import com.example.dailytrack_mobile.presentation.util.BiometricHelper
import com.example.dailytrack_mobile.presentation.util.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinVerifyDialog(
    appLockManager: AppLockManager,
    title: String = "Enter PIN",
    subtitle: String = "Enter your 4-digit security PIN to proceed",
    showBiometricOption: Boolean = true,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dims = Dimens.current

    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = "Use PIN",
            allowDeviceCredential = false,
            onSuccess = {
                onSuccess()
            },
            onError = { _, _ -> },
            onFailed = {}
        )
    }

    LaunchedEffect(Unit) {
        if (showBiometricOption && isBiometricAvailable) {
            triggerBiometric()
        }
    }

    fun handleDigit(digit: Int) {
        if (enteredPin.length >= 4) return
        isPinError = false
        errorMessage = null

        val updated = enteredPin + digit.toString()
        enteredPin = updated

        if (updated.length == 4) {
            coroutineScope.launch {
                val isValid = appLockManager.validatePin(updated)
                if (isValid) {
                    onSuccess()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPinError = true
                    errorMessage = "Incorrect PIN. Try again."
                    delay(300)
                    enteredPin = ""
                }
            }
        }
    }

    fun handleDelete() {
        isPinError = false
        errorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.size(36.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // PIN Dots
                PinDots(
                    enteredCount = enteredPin.length,
                    isError = isPinError
                )

                // Error text
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                }

                // Keypad
                PinKeypad(
                    onDigitClick = { handleDigit(it) },
                    onDeleteClick = { handleDelete() },
                    onBiometricClick = if (showBiometricOption && isBiometricAvailable) {
                        { triggerBiometric() }
                    } else null,
                    showBiometricButton = showBiometricOption && isBiometricAvailable
                )
            }
        }
    }
}
