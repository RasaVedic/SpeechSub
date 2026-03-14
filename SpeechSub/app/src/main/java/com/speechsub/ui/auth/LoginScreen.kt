package com.speechsub.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * LoginScreen — Email/password login with Firebase Auth.
 *
 * Features:
 * - Email + password fields with validation
 * - Password visibility toggle
 * - Forgot password link
 * - Error messages
 * - Loading state during sign-in
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess    : () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel         : AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager    = LocalFocusManager.current

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF1A1040))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo
            Text("🎬", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Sign in to SpeechSub",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(40.dp))

            // Glass card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Email field
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Email") },
                        leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        leadingIcon   = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon  = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                                  else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                              else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.signIn(email, password)
                            }
                        ),
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(12.dp)
                    )

                    // Forgot password
                    TextButton(
                        onClick  = { viewModel.sendPasswordReset(email) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Error message
                    AnimatedVisibility(uiState is AuthUiState.Error) {
                        val error = (uiState as? AuthUiState.Error)?.message ?: ""
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text     = error,
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style    = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Login button
                    Button(
                        onClick  = { viewModel.signIn(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled  = uiState !is AuthUiState.Loading,
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color    = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign In", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sign up link
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Don't have an account?",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                TextButton(onClick = onNavigateToSignUp) {
                    Text("Sign Up", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
