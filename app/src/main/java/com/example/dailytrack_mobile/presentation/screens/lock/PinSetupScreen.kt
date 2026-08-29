package com.example.dailytrack_mobile.presentation.screens.lock

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.lock.components.PinDots
import com.example.dailytrack_mobile.presentation.screens.lock.components.PinKeypad
import kotlinx.coroutines.delay

enum class PinSetupStep {
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

@Composable
fun PinSetupScreen(
    title: String = "Set Custom PIN",
    subtitle: String = "Enter a 4-digit PIN to secure your app",
    onPinCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(PinSetupStep.ENTER_NEW_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val currentEnteredPin = if (step == PinSetupStep.ENTER_NEW_PIN) firstPin else secondPin

    fun handleDigit(digit: Int) {
        if (currentEnteredPin.length >= 4) return
        isError = false
        errorMessage = null

        val updated = currentEnteredPin + digit.toString()
        if (step == PinSetupStep.ENTER_NEW_PIN) {
            firstPin = updated
            if (updated.length == 4) {
                // Move to confirm step
                step = PinSetupStep.CONFIRM_NEW_PIN
            }
        } else {
            secondPin = updated
            if (updated.length == 4) {
                if (updated == firstPin) {
                    onPinCreated(updated)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isError = true
                    errorMessage = "PINs do not match. Try again."
                    secondPin = ""
                    // Reset to first step after brief delay or stay at confirm
                    step = PinSetupStep.ENTER_NEW_PIN
                    firstPin = ""
                }
            }
        }
    }

    fun handleDelete() {
        isError = false
        errorMessage = null
        if (step == PinSetupStep.ENTER_NEW_PIN) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (secondPin.isNotEmpty()) {
                secondPin = secondPin.dropLast(1)
            } else {
                // If empty in confirm step, go back to enter step
                step = PinSetupStep.ENTER_NEW_PIN
                firstPin = ""
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (step == PinSetupStep.ENTER_NEW_PIN) title else "Confirm PIN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (step == PinSetupStep.ENTER_NEW_PIN) subtitle else "Re-enter your 4-digit PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                PinDots(
                    pinLength = 4,
                    enteredCount = currentEnteredPin.length,
                    isError = isError
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            PinKeypad(
                onDigitClick = { handleDigit(it) },
                onDeleteClick = { handleDelete() },
                showBiometricButton = false
            )
        }
    }
}
