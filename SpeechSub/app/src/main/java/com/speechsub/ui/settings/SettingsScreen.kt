package com.speechsub.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speechsub.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack : () -> Unit,
    onSignedOut    : () -> Unit,
    viewModel      : SettingsViewModel = hiltViewModel()
) {
    val defaultLanguage   by viewModel.defaultLanguage.collectAsStateWithLifecycle()
    val defaultFont       by viewModel.defaultFont.collectAsStateWithLifecycle()
    val defaultFontSize   by viewModel.defaultFontSize.collectAsStateWithLifecycle()
    val cloudSyncEnabled  by viewModel.cloudSyncEnabled.collectAsStateWithLifecycle()
    val snackbarMessage   by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val signedOut         by viewModel.signedOut.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showSignOutDialog    by remember { mutableStateOf(false) }
    var showLanguageDialog   by remember { mutableStateOf(false) }
    var showFontDialog       by remember { mutableStateOf(false) }

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost  = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D0D1A), Color(0xFF1A1040), Color(0xFF0D0D1A))
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {

                item { SettingsSectionHeader("Account") }
                item {
                    AccountCard(
                        name    = viewModel.userName,
                        email   = viewModel.userEmail,
                        isLoggedIn = viewModel.isLoggedIn,
                        onSignOut  = { showSignOutDialog = true }
                    )
                }

                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("Caption Defaults") }

                item {
                    SettingsItemRow(
                        icon        = Icons.Outlined.Language,
                        title       = "Default Language",
                        subtitle    = languageOptions.firstOrNull { it.first == defaultLanguage }?.second
                                      ?: defaultLanguage,
                        onClick     = { showLanguageDialog = true }
                    )
                }
                item {
                    SettingsItemRow(
                        icon        = Icons.Outlined.TextFields,
                        title       = "Default Font",
                        subtitle    = fontOptions.firstOrNull { it.first == defaultFont }?.second
                                      ?: defaultFont,
                        onClick     = { showFontDialog = true }
                    )
                }
                item {
                    FontSizeSlider(
                        size     = defaultFontSize,
                        onChange = viewModel::setDefaultFontSize
                    )
                }

                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("Storage") }

                item {
                    SettingsSwitchRow(
                        icon      = Icons.Outlined.Cloud,
                        title     = "Cloud Sync",
                        subtitle  = if (viewModel.isLoggedIn)
                            "Automatically back up captions to Firebase"
                        else
                            "Log in to enable cloud sync",
                        enabled   = viewModel.isLoggedIn,
                        checked   = cloudSyncEnabled,
                        onChecked = viewModel::setCloudSyncEnabled
                    )
                }

                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("About") }
                item {
                    SettingsItemRow(
                        icon     = Icons.Outlined.Info,
                        title    = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        onClick  = {}
                    )
                }
                item {
                    SettingsItemRow(
                        icon     = Icons.Outlined.Code,
                        title    = "Open Source",
                        subtitle = "github.com/your-username/SpeechSub",
                        onClick  = {}
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out") },
                text  = { Text("Are you sure you want to sign out? Your local captions will remain on this device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.signOut()
                        }
                    ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showLanguageDialog) {
            PickerDialog(
                title   = "Default Language",
                options = languageOptions,
                selected = defaultLanguage,
                onSelect = { viewModel.setDefaultLanguage(it); showLanguageDialog = false },
                onDismiss = { showLanguageDialog = false }
            )
        }

        if (showFontDialog) {
            PickerDialog(
                title   = "Default Font",
                options = fontOptions,
                selected = defaultFont,
                onSelect = { viewModel.setDefaultFont(it); showFontDialog = false },
                onDismiss = { showFontDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text   = title.uppercase(),
        style  = MaterialTheme.typography.labelSmall,
        color  = MaterialTheme.colorScheme.primary.copy(0.8f),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun AccountCard(
    name: String, email: String, isLoggedIn: Boolean, onSignOut: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = if (isLoggedIn) name.firstOrNull()?.uppercaseChar()?.toString() ?: "U" else "?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isLoggedIn) name else "Not Logged In",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    if (isLoggedIn) email else "Log in to sync captions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f)
                )
            }

            if (isLoggedIn) {
                IconButton(onClick = onSignOut) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.error.copy(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon    : ImageVector,
    title   : String,
    subtitle: String,
    onClick : () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f))
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    enabled  : Boolean,
    checked  : Boolean,
    onChecked: (Boolean) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f))
            }
            Switch(
                checked         = checked,
                onCheckedChange = onChecked,
                enabled         = enabled,
            )
        }
    }
}

@Composable
private fun FontSizeSlider(size: Float, onChange: (Float) -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FormatSize, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("Default Font Size", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${size.toInt()} sp", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value        = size,
                onValueChange = onChange,
                valueRange   = 12f..24f,
                steps        = 11,
                modifier     = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PickerDialog(
    title    : String,
    options  : List<Pair<String, String>>,
    selected : String,
    onSelect : (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.distinct().forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == key,
                            onClick  = { onSelect(key) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
