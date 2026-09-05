package com.example.dailytrack_mobile.presentation.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.dailytrack_mobile.presentation.util.Dimens
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val WEB_CLIENT_ID = "68900020784-s98hgjb235573iga5db1bprubs173ghb.apps.googleusercontent.com"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dims = Dimens.current

    LaunchedEffect(state.isLoginSuccess) {
        if (state.isLoginSuccess) {
            onLoginSuccess()
        }
    }

    // Google Sign-In intent fallback launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("DAILYTRACK_AUTH", "googleSignInLauncher resultCode = ${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            android.util.Log.d("DAILYTRACK_AUTH", "Account: ${account.email}, hasIdToken: ${idToken != null}")
            if (idToken != null) {
                coroutineScope.launch {
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(credential).await()
                        val firebaseIdToken = authResult.user?.getIdToken(false)?.await()?.token
                        if (firebaseIdToken != null) {
                            onAction(
                                LoginAction.OnGoogleTokenReceived(
                                    firebaseIdToken = firebaseIdToken,
                                    email = account.email ?: "",
                                    displayName = account.displayName,
                                    photoUrl = account.photoUrl?.toString()
                                )
                            )
                        } else {
                            onAction(LoginAction.OnLoginFailed("Unable to obtain Firebase token."))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DAILYTRACK_AUTH", "Firebase auth error", e)
                        onAction(LoginAction.OnLoginFailed("Firebase error: ${e.message}"))
                    }
                }
            } else {
                onAction(LoginAction.OnLoginFailed("Google Sign-In succeeded but returned no ID token."))
            }
        } catch (e: ApiException) {
            android.util.Log.e("DAILYTRACK_AUTH", "Google Sign-In ApiException statusCode = ${e.statusCode}", e)
            val hint = when (e.statusCode) {
                10 -> " (Developer Error: SHA-1 fingerprint missing or mismatched in Firebase Console for com.example.dailytrack_mobile)"
                7 -> " (Network Error: Check internet connection)"
                12500 -> " (Sign-in failed: Google Play Services issue)"
                else -> ""
            }
            onAction(LoginAction.OnLoginFailed("Google Sign-In error (${e.statusCode})$hint"))
        } catch (e: Exception) {
            android.util.Log.e("DAILYTRACK_AUTH", "General error processing Google Sign-In", e)
            onAction(LoginAction.OnLoginFailed("Sign-in failed: ${e.message}"))
        }
    }

    fun initiateGoogleLogin() {
        val activity = context as? Activity ?: return
        try {
            android.util.Log.d("DAILYTRACK_AUTH", "Launching GoogleSignInClient directly...")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("DAILYTRACK_AUTH", "Failed to launch Google Sign-In", e)
            onAction(LoginAction.OnLoginFailed("Could not start Google Sign-In: ${e.message}"))
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient radial glow behind the card
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp)),
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "DailyTrack Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "DailyTrack",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Personal Finance & Daily Analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Main Authentication Card
            Card(
                shape = RoundedCornerShape(dims.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dims.cardCornerRadius))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in to your account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Connect with your authorized Google account to sync your personal finances and habits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Error Box
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = state.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Google Sign-In Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (state.isLoading) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !state.isLoading) {
                                onAction(LoginAction.OnClearError)
                                initiateGoogleLogin()
                            },
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (state.isLoading) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Signing in...",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                GoogleLogoIcon(modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Only authorized Google accounts are permitted.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Demo Mode option
            TextButton(
                onClick = {
                    onAction(LoginAction.OnContinueDemoMode)
                    onLoginSuccess()
                }
            ) {
                Text(
                    text = "Explore in Demo Mode",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Authentic 4-color Google 'G' vector logo
 */
@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Blue right / top-right curve
        drawPath(
            path = Path().apply {
                moveTo(w * 0.95f, h * 0.5f)
                cubicTo(w * 0.95f, h * 0.44f, w * 0.94f, h * 0.38f, w * 0.93f, h * 0.33f)
                lineTo(w * 0.5f, h * 0.33f)
                lineTo(w * 0.5f, h * 0.52f)
                lineTo(w * 0.76f, h * 0.52f)
                cubicTo(w * 0.74f, h * 0.61f, w * 0.69f, h * 0.69f, w * 0.61f, h * 0.74f)
                lineTo(w * 0.77f, h * 0.86f)
                cubicTo(w * 0.87f, h * 0.77f, w * 0.95f, h * 0.64f, w * 0.95f, h * 0.5f)
                close()
            },
            color = Color(0xFF4285F4)
        )

        // Green bottom curve
        drawPath(
            path = Path().apply {
                moveTo(w * 0.5f, h * 0.95f)
                cubicTo(w * 0.63f, h * 0.95f, w * 0.74f, h * 0.91f, w * 0.82f, h * 0.84f)
                lineTo(w * 0.66f, h * 0.72f)
                cubicTo(w * 0.62f, h * 0.75f, w * 0.56f, h * 0.77f, w * 0.5f, h * 0.77f)
                cubicTo(w * 0.37f, h * 0.77f, w * 0.27f, h * 0.69f, w * 0.23f, h * 0.57f)
                lineTo(w * 0.07f, h * 0.70f)
                cubicTo(w * 0.15f, h * 0.85f, w * 0.31f, h * 0.95f, w * 0.5f, h * 0.95f)
                close()
            },
            color = Color(0xFF34A853)
        )

        // Yellow bottom-left curve
        drawPath(
            path = Path().apply {
                moveTo(w * 0.23f, h * 0.57f)
                cubicTo(w * 0.22f, h * 0.54f, w * 0.21f, h * 0.51f, w * 0.21f, h * 0.48f)
                cubicTo(w * 0.21f, h * 0.45f, w * 0.22f, h * 0.42f, w * 0.23f, h * 0.39f)
                lineTo(w * 0.07f, h * 0.26f)
                cubicTo(w * 0.03f, h * 0.33f, w * 0.01f, h * 0.40f, w * 0.01f, h * 0.48f)
                cubicTo(w * 0.01f, h * 0.56f, w * 0.03f, h * 0.63f, w * 0.07f, h * 0.70f)
                lineTo(w * 0.23f, h * 0.57f)
                close()
            },
            color = Color(0xFFFBBC05)
        )

        // Red top curve
        drawPath(
            path = Path().apply {
                moveTo(w * 0.5f, h * 0.19f)
                cubicTo(w * 0.58f, h * 0.19f, w * 0.65f, h * 0.22f, w * 0.71f, h * 0.27f)
                lineTo(w * 0.83f, h * 0.15f)
                cubicTo(w * 0.75f, h * 0.08f, w * 0.64f, h * 0.03f, w * 0.5f, h * 0.03f)
                cubicTo(w * 0.31f, h * 0.03f, w * 0.15f, h * 0.13f, w * 0.07f, h * 0.28f)
                lineTo(w * 0.23f, h * 0.41f)
                cubicTo(w * 0.27f, h * 0.29f, w * 0.37f, h * 0.19f, w * 0.5f, h * 0.19f)
                close()
            },
            color = Color(0xFFEA4335)
        )
    }
}
