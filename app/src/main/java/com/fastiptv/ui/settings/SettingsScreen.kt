package com.fastiptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fastiptv.BuildConfig
import com.fastiptv.ota.OtaUpdateState
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.LiveRed
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

enum class SettingsEditingField {
    NONE, HOST, PORT, USERNAME, PASSWORD
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null
) {
    val currentConfig by viewModel.currentConfig.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val preferredStreamFormat by viewModel.preferredStreamFormat.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var host by remember(currentConfig) { mutableStateOf(currentConfig?.host ?: "") }
    var port by remember(currentConfig) { mutableStateOf(currentConfig?.port?.toString() ?: "8080") }
    var username by remember(currentConfig) { mutableStateOf(currentConfig?.username ?: "") }
    var password by remember(currentConfig) { mutableStateOf(currentConfig?.password ?: "") }

    var editingField by remember { mutableStateOf(SettingsEditingField.NONE) }
    val hostFocusRequester = remember { FocusRequester() }
    val portFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = editingField != SettingsEditingField.NONE) {
        val target = when (editingField) {
            SettingsEditingField.HOST -> hostFocusRequester
            SettingsEditingField.PORT -> portFocusRequester
            SettingsEditingField.USERNAME -> usernameFocusRequester
            SettingsEditingField.PASSWORD -> passwordFocusRequester
            SettingsEditingField.NONE -> null
        }
        keyboardController?.hide()
        try {
            target?.requestFocus()
        } catch (_: Exception) {}
        editingField = SettingsEditingField.NONE
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Configure your Xtream Codes IPTV provider credentials to load live channels, movies, and EPG.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvCredentialField(
                    label = "Server Host / URL",
                    value = host,
                    placeholder = "e.g. your-provider.com",
                    isEditing = editingField == SettingsEditingField.HOST,
                    fieldFocusRequester = hostFocusRequester,
                    upFocusRequester = topNavFocusRequester,
                    rightFocusRequester = portFocusRequester,
                    downFocusRequester = usernameFocusRequester,
                    onValueChange = { host = it },
                    onClickToEdit = { editingField = SettingsEditingField.HOST },
                    onDoneEditing = { editingField = SettingsEditingField.NONE },
                    modifier = Modifier.weight(3f)
                )

                TvCredentialField(
                    label = "Port",
                    value = port,
                    placeholder = "8080",
                    isEditing = editingField == SettingsEditingField.PORT,
                    keyboardType = KeyboardType.Number,
                    fieldFocusRequester = portFocusRequester,
                    upFocusRequester = topNavFocusRequester,
                    leftFocusRequester = hostFocusRequester,
                    downFocusRequester = passwordFocusRequester,
                    onValueChange = { port = it },
                    onClickToEdit = { editingField = SettingsEditingField.PORT },
                    onDoneEditing = { editingField = SettingsEditingField.NONE },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvCredentialField(
                    label = "Username",
                    value = username,
                    placeholder = "Enter username",
                    isEditing = editingField == SettingsEditingField.USERNAME,
                    fieldFocusRequester = usernameFocusRequester,
                    upFocusRequester = hostFocusRequester,
                    rightFocusRequester = passwordFocusRequester,
                    downFocusRequester = saveButtonFocusRequester,
                    onValueChange = { username = it },
                    onClickToEdit = { editingField = SettingsEditingField.USERNAME },
                    onDoneEditing = { editingField = SettingsEditingField.NONE },
                    modifier = Modifier.weight(1f)
                )

                TvCredentialField(
                    label = "Password",
                    value = password,
                    placeholder = "Enter password",
                    isEditing = editingField == SettingsEditingField.PASSWORD,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    fieldFocusRequester = passwordFocusRequester,
                    upFocusRequester = portFocusRequester,
                    leftFocusRequester = usernameFocusRequester,
                    downFocusRequester = saveButtonFocusRequester,
                    onValueChange = { password = it },
                    onClickToEdit = { editingField = SettingsEditingField.PASSWORD },
                    onDoneEditing = { editingField = SettingsEditingField.NONE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        viewModel.saveConfig(host, port, username, password)
                    },
                    modifier = Modifier
                        .focusRequester(saveButtonFocusRequester)
                        .focusProperties {
                            up = usernameFocusRequester
                        },
                    colors = ButtonDefaults.colors(
                        containerColor = AccentBlue,
                        focusedContainerColor = Color(0xFF2563EB),
                        contentColor = TextWhite,
                        focusedContentColor = TextWhite
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (isLoading) "Testing Connection..." else "Save & Connect",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        viewModel.refreshAllCategories()
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = DarkSurfaceElevated,
                        focusedContainerColor = AccentBlue,
                        contentColor = TextWhite,
                        focusedContentColor = TextWhite
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(text = "Sync Content")
                }

                Button(
                    onClick = {
                        viewModel.clearSession()
                        host = ""
                        port = "8080"
                        username = ""
                        password = ""
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = DarkSurface,
                        focusedContainerColor = LiveRed.copy(alpha = 0.85f),
                        contentColor = LiveRed,
                        focusedContentColor = TextWhite
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(text = "Clear Credentials")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streaming Preferences Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Streaming Protocol & Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Choose your preferred streaming protocol for Live TV. TS provides lowest latency; HLS uses segmented buffering for network resilience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val isTs = preferredStreamFormat == "ts"
                        androidx.compose.material3.Surface(
                            onClick = { viewModel.setPreferredStreamFormat("ts") },
                            color = if (isTs) AccentBlue.copy(alpha = 0.25f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isTs) AccentBlue else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = if (isTs) "● MPEG-TS (.ts) [Active]" else "○ MPEG-TS (.ts)",
                                color = if (isTs) AccentBlue else TextWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }

                        val isHls = preferredStreamFormat == "m3u8"
                        androidx.compose.material3.Surface(
                            onClick = { viewModel.setPreferredStreamFormat("m3u8") },
                            color = if (isHls) AccentBlue.copy(alpha = 0.25f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isHls) AccentBlue else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = if (isHls) "● HLS (.m3u8) [Active]" else "○ HLS (.m3u8)",
                                color = if (isHls) AccentBlue else TextWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Software Updates (Over-The-Air) Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Software Updates (Over-The-Air)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Current Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Powered by GitHub Releases",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            colors = ButtonDefaults.colors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = TextWhite,
                                focusedContainerColor = AccentBlue,
                                focusedContentColor = TextWhite
                            )
                        ) {
                            Text("Check for Updates")
                        }
                    }

                    when (val s = updateState) {
                        is OtaUpdateState.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Checking for new releases on GitHub...",
                                    color = AccentBlue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        is OtaUpdateState.UpToDate -> {
                            Text(
                                text = "✓ FastIPTV is up to date (v${s.currentVersion})",
                                color = Color(0xFF81C784),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is OtaUpdateState.UpdateAvailable -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentBlue.copy(alpha = 0.15f))
                                    .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "★ New Update Available: v${s.updateInfo.versionName}",
                                    color = Color(0xFF60A5FA),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (s.updateInfo.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = s.updateInfo.releaseNotes,
                                        color = TextWhite.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 4
                                    )
                                }
                                Button(
                                    onClick = { viewModel.downloadAndInstallUpdate(s.updateInfo) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = AccentBlue,
                                        contentColor = TextWhite,
                                        focusedContainerColor = Color(0xFF2563EB),
                                        focusedContentColor = TextWhite
                                    )
                                ) {
                                    Text("Download & Install Update")
                                }
                            }
                        }
                        is OtaUpdateState.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Downloading update: ${s.progressPercent}%",
                                    color = AccentBlue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { s.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AccentBlue,
                                    trackColor = DarkSurfaceElevated
                                )
                            }
                        }
                        is OtaUpdateState.ReadyToInstall -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "✓ Update downloaded and ready to install.",
                                    color = Color(0xFF81C784),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = { viewModel.installDownloadedApk(s.apkFile) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = TextWhite,
                                        focusedContainerColor = Color(0xFF22C55E),
                                        focusedContentColor = TextWhite
                                    )
                                ) {
                                    Text("Install Now")
                                }
                            }
                        }
                        is OtaUpdateState.Error -> {
                            Text(
                                text = "⚠ Update Check: ${s.message}",
                                color = LiveRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OtaUpdateState.Idle -> { /* Nothing displayed */ }
                    }
                }
            }

            when (val s = syncStatus) {
                is com.fastiptv.data.sync.SyncStatus.Syncing -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue.copy(alpha = 0.15f))
                            .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = s.message,
                                color = TextWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is com.fastiptv.data.sync.SyncStatus.Success -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "✓ ${s.message}",
                            color = Color(0xFF81C784),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is com.fastiptv.data.sync.SyncStatus.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LiveRed.copy(alpha = 0.2f))
                            .border(1.dp, LiveRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "⚠ ${s.message}",
                            color = LiveRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                com.fastiptv.data.sync.SyncStatus.Idle -> {
                    if (!statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (statusMessage?.contains("Error") == true || statusMessage?.contains("failed") == true) LiveRed else AccentBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCredentialField(
    label: String,
    value: String,
    placeholder: String,
    isEditing: Boolean,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    fieldFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onValueChange: (String) -> Unit,
    onClickToEdit: () -> Unit,
    onDoneEditing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editTextFieldFocusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            try {
                editTextFieldFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    val handleDone: () -> Unit = {
        keyboardController?.hide()
        try {
            fieldFocusRequester.requestFocus()
        } catch (_: Exception) {}
        onDoneEditing()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused || isEditing) Color(0xFF1E293B) else DarkSurface)
            .border(
                width = if (isFocused || isEditing) 2.5.dp else 1.dp,
                color = if (isFocused || isEditing) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .focusRequester(fieldFocusRequester)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .focusProperties {
                if (!isEditing) {
                    upFocusRequester?.let { up = it }
                    downFocusRequester?.let { down = it }
                    leftFocusRequester?.let { left = it }
                    rightFocusRequester?.let { right = it }
                }
            }
            .focusable()
            .clickable {
                if (!isEditing) {
                    onClickToEdit()
                }
            }
            .onKeyEvent { event ->
                if (!isEditing && event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onClickToEdit()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFocused || isEditing) Color(0xFF60A5FA) else TextMuted,
                    fontWeight = if (isFocused || isEditing) FontWeight.Bold else FontWeight.Medium
                )
                if (isEditing) {
                    Text(
                        text = "Press Done or Back to save",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (isFocused) {
                    Text(
                        text = "⏎ Press OK to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isEditing) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(editTextFieldFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                handleDone()
                                true
                            } else if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                                handleDone()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(AccentBlue),
                    singleLine = true,
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleDone() }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                val displayValue = when {
                    value.isEmpty() -> placeholder
                    isPassword -> "•".repeat(minOf(value.length, 24))
                    else -> value
                }

                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isEmpty()) TextMuted.copy(alpha = 0.5f) else TextWhite,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
