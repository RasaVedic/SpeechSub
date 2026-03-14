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
 * SignUpScreen — creates a new Firebase Auth account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess   : () -> Unit,
    onNavigateToLogin : () -> Unit,
    viewModel         : AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager    = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onSignUpSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D1A), Color(0xFF1A1040))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text("🎬", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Join SpeechSub today",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Name
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))

                    // Confirm password
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (password == confirmPassword) viewModel.signUp(email, password, name)
                        })
                    )

                    AnimatedVisibility(uiState is AuthUiState.Error) {
                        val error = (uiState as? AuthUiState.Error)?.message ?: ""
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (password == confirmPassword) {
                                viewModel.signUp(email, password, name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState !is AuthUiState.Loading && password == confirmPassword,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Create Account")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
