package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.data.local.security.LockType
import com.example.dailytrack_mobile.presentation.screens.lock.PinSetupScreen
import com.example.dailytrack_mobile.presentation.util.BiometricHelper
import com.example.dailytrack_mobile.presentation.util.Dimens

@Composable
fun AppLockSettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isBiometricSupported = remember { BiometricHelper.isBiometricAvailable(context) }
    var isSettingUpPin by remember { mutableStateOf(false) }
    var isChangingPin by remember { mutableStateOf(false) }

    if (isSettingUpPin || isChangingPin) {
        PinSetupScreen(
            title = if (isChangingPin) "Change PIN" else "Set Custom PIN",
            subtitle = if (isChangingPin) "Enter your new 4-digit PIN" else "Enter a 4-digit PIN to secure your app",
            onPinCreated = { newPin ->
                onAction(SettingsAction.OnSaveCustomPin(newPin))
                isSettingUpPin = false
                isChangingPin = false
            },
            onCancel = {
                isSettingUpPin = false
                isChangingPin = false
            }
        )
        return
    }

    val dims = Dimens.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Lock DailyTrack",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Keep your data private by adding an extra layer of security",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }

            // Ways to lock section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ways to lock",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Same as screen lock
                LockOptionCard(
                    icon = Icons.Default.Fingerprint,
                    title = "Same as screen lock",
                    subtitle = "Use fingerprint, face, or device screen lock",
                    isSelected = state.lockType == LockType.SYSTEM,
                    onClick = {
                        onAction(SettingsAction.OnLockTypeSelected(LockType.SYSTEM))
                    }
                )

                // Option 2: Custom PIN
                LockOptionCard(
                    icon = Icons.Default.Password,
                    title = "Custom PIN",
                    subtitle = if (state.hasCustomPin) "4-digit passcode configured" else "Set up a 4-digit passcode",
                    isSelected = state.lockType == LockType.PIN,
                    onClick = {
                        if (!state.hasCustomPin) {
                            isSettingUpPin = true
                        }
                        onAction(SettingsAction.OnLockTypeSelected(LockType.PIN))
                    }
                )
            }

            // Extra Settings for Custom PIN
            AnimatedVisibility(
                visible = state.lockType == LockType.PIN,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(dims.cardCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Change PIN Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isChangingPin = true }
                                .padding(horizontal = dims.cardInnerPadding, vertical = dims.itemSpacingLarge),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(dims.iconSizeMedium)
                            )
                            Spacer(Modifier.width(dims.itemSpacingLarge))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.hasCustomPin) "Change PIN" else "Set up PIN",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Update your 4-digit passcode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(dims.iconSizeSmall + 4.dp)
                            )
                        }

                        if (isBiometricSupported) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            // Fingerprint toggle with PIN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(SettingsAction.OnBiometricWithPinToggled(!state.isBiometricWithPinEnabled))
                                    }
                                    .padding(horizontal = dims.cardInnerPadding, vertical = dims.itemSpacingMedium + 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(dims.iconSizeMedium)
                                )
                                Spacer(Modifier.width(dims.itemSpacingLarge))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unlock with fingerprint",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Use fingerprint alongside PIN",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = state.isBiometricWithPinEnabled,
                                    onCheckedChange = {
                                        onAction(SettingsAction.OnBiometricWithPinToggled(it))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isSelected)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else
            null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    }
}
