package com.example.fable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fable.data.remote.ServerUrlValidator
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile
import com.example.fable.ui.components.StoryCover
import com.example.fable.ui.viewmodel.AccountUiState
import com.example.fable.ui.viewmodel.ServerConfigUiState

@Composable
fun AccountScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onRequestPasswordReset: (String) -> Unit,
    onConfirmPasswordReset: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onStorySelected: (Story) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteAccount: () -> Unit = {},
    onEditStory: (Story) -> Unit = {},
    onDeleteStory: (Story) -> Unit = {},
    serverConfigState: ServerConfigUiState? = null,
    onApplyServerUrl: (String) -> Unit = {},
    onResetServerUrl: () -> Unit = {},
    onTestServerHealth: () -> Unit = {}
) {
    var isRegistration by rememberSaveable { mutableStateOf(false) }
    var isRecoveringPassword by rememberSaveable { mutableStateOf(false) }
    var isEditingProfile by rememberSaveable(state.user?.id) { mutableStateOf(false) }
    var showDeleteAccountConfirmation by rememberSaveable { mutableStateOf(false) }
    var storyPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.name.orEmpty()) }
    var handle by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.handle.orEmpty()) }
    var bio by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.bio.orEmpty()) }
    var resetToken by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Account", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                when {
                    state.user != null -> SignedInAccount(
                        user = state.user,
                        isEditing = isEditingProfile,
                        isBusy = state.isBusy,
                        name = name,
                        onNameChange = { name = it.take(80) },
                        handle = handle,
                        onHandleChange = { handle = it.take(40) },
                        bio = bio,
                        onBioChange = { bio = it.take(500) },
                        onEdit = { isEditingProfile = true },
                        onSave = { onSaveProfile(name, handle, bio) },
                        onCancel = {
                            name = state.user?.name.orEmpty()
                            handle = state.user?.handle.orEmpty()
                            bio = state.user?.bio.orEmpty()
                            isEditingProfile = false
                        },
                        onSignOut = onSignOut,
                        onRequestDeleteAccount = { showDeleteAccountConfirmation = true }
                    )
                    isRecoveringPassword -> PasswordRecoveryForm(
                        email = email,
                        onEmailChange = { email = it },
                        token = resetToken,
                        onTokenChange = { resetToken = it },
                        password = password,
                        onPasswordChange = { password = it },
                        isBusy = state.isBusy,
                        onRequest = { onRequestPasswordReset(email) },
                        onConfirm = { onConfirmPasswordReset(resetToken, password) },
                        onBackToSignIn = { isRecoveringPassword = false }
                    )
                    else -> AuthForm(
                        isRegistration = isRegistration,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        name = name,
                        onNameChange = { name = it.take(80) },
                        handle = handle,
                        onHandleChange = { handle = it.take(40) },
                        isBusy = state.isBusy,
                        onModeChange = { isRegistration = it },
                        onSignIn = { onSignIn(email, password) },
                        onRegister = { onRegister(email, password, name, handle) },
                        onForgotPassword = { isRecoveringPassword = true }
                    )
                }
            }
            if (state.user != null) {
                item {
                    Text("Your published stories", style = MaterialTheme.typography.titleLarge)
                }
                when {
                    state.isLoadingPublishedStories -> item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Loading stories", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.publishedStoriesErrorMessage != null -> item {
                        Text(
                            state.publishedStoriesErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    state.publishedStories.isEmpty() -> item {
                        Text("No published stories yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> items(state.publishedStories, key = Story::id) { story ->
                        PublishedStoryRow(
                            story = story,
                            isBusy = state.isBusy,
                            onClick = { onStorySelected(story) },
                            onEdit = { onEditStory(story) },
                            onDelete = { storyPendingDeletion = story.id }
                        )
                    }
                }
            }
            if (state.errorMessage != null) {
                item {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.noticeMessage != null) {
                item {
                    Text(state.noticeMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (serverConfigState?.isRuntimeSwitchEnabled == true) {
                item {
                    DeveloperServerSection(
                        state = serverConfigState,
                        onApplyServerUrl = onApplyServerUrl,
                        onResetServerUrl = onResetServerUrl,
                        onTestServerHealth = onTestServerHealth
                    )
                }
            }
        }
    }

    if (showDeleteAccountConfirmation && state.user != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmation = false },
            title = { Text("Delete account?") },
            text = {
                Text(
                    "This permanently deletes ${state.user.email}, your published stories, your synced shelf, and your reading history. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountConfirmation = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val pendingStory = storyPendingDeletion?.let { id ->
        state.publishedStories.firstOrNull { it.id == id }
    }
    if (pendingStory != null) {
        AlertDialog(
            onDismissRequest = { storyPendingDeletion = null },
            title = { Text("Delete \"${pendingStory.title}\"?") },
            text = {
                Text("This permanently removes this story and all of its chapters from Fable. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storyPendingDeletion = null
                        onDeleteStory(pendingStory)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PublishedStoryRow(
    story: Story,
    isBusy: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StoryCover(story, Modifier.size(width = 54.dp, height = 74.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(story.genre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(story.synopsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = onEdit,
                enabled = !isBusy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit story")
            }
            IconButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete story",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DeveloperServerSection(
    state: ServerConfigUiState,
    onApplyServerUrl: (String) -> Unit,
    onResetServerUrl: () -> Unit,
    onTestServerHealth: () -> Unit
) {
    var serverUrlInput by rememberSaveable(state.activeBaseUrl) { mutableStateOf(state.activeBaseUrl) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "DEVELOPER SERVER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                state.activeBaseUrl,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.activeBaseUrl == ServerUrlValidator.HOSTED_RENDER_BASE_URL,
                    onClick = {
                        serverUrlInput = ServerUrlValidator.HOSTED_RENDER_BASE_URL
                        onApplyServerUrl(ServerUrlValidator.HOSTED_RENDER_BASE_URL)
                    },
                    label = { Text("Hosted Render") }
                )
                FilterChip(
                    selected = state.activeBaseUrl == ServerUrlValidator.EMULATOR_LOCAL_BASE_URL,
                    onClick = {
                        serverUrlInput = ServerUrlValidator.EMULATOR_LOCAL_BASE_URL
                        onApplyServerUrl(ServerUrlValidator.EMULATOR_LOCAL_BASE_URL)
                    },
                    label = { Text("Emulator") }
                )
                FilterChip(
                    selected = state.activeBaseUrl == state.defaultBaseUrl,
                    onClick = {
                        serverUrlInput = state.defaultBaseUrl
                        onResetServerUrl()
                    },
                    label = { Text("Build Default") }
                )
            }
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = { serverUrlInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API base URL") },
                placeholder = { Text(ServerUrlValidator.EMULATOR_LOCAL_BASE_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onApplyServerUrl(serverUrlInput) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
                OutlinedButton(
                    onClick = onTestServerHealth,
                    enabled = !state.isCheckingHealth
                ) {
                    Text(if (state.isCheckingHealth) "Checking..." else "Test health")
                }
            }
            if (state.errorMessage != null) {
                Text(
                    state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.healthStatusMessage != null) {
                Text(
                    state.healthStatusMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AuthForm(
    isRegistration: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    handle: String,
    onHandleChange: (String) -> Unit,
    isBusy: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (isRegistration) "Create account" else "Sign in", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isRegistration, onClick = { onModeChange(false) }, label = { Text("Sign in") })
            FilterChip(selected = isRegistration, onClick = { onModeChange(true) }, label = { Text("Register") })
        }
        if (isRegistration) {
            OutlinedTextField(name, onNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            OutlinedTextField(handle, onHandleChange, modifier = Modifier.fillMaxWidth(), label = { Text("Handle") }, singleLine = true)
        }
        OutlinedTextField(email, onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(
            password,
            onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            supportingText = if (isRegistration) ({ Text("At least 12 characters") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            onClick = if (isRegistration) onRegister else onSignIn,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isRegistration) "Create account" else "Sign in") }
        if (!isRegistration) {
            TextButton(onClick = onForgotPassword, enabled = !isBusy) { Text("Forgot password?") }
        }
    }
}

@Composable
private fun PasswordRecoveryForm(
    email: String,
    onEmailChange: (String) -> Unit,
    token: String,
    onTokenChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isBusy: Boolean,
    onRequest: () -> Unit,
    onConfirm: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Password recovery", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(email, onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text("Account email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Button(onClick = onRequest, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) { Text("Send recovery email") }
        OutlinedTextField(token, onTokenChange, modifier = Modifier.fillMaxWidth(), label = { Text("Reset token") }, singleLine = true)
        OutlinedTextField(password, onPasswordChange, modifier = Modifier.fillMaxWidth(), label = { Text("New password") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        OutlinedButton(onClick = onConfirm, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) { Text("Update password") }
        TextButton(onClick = onBackToSignIn) { Text("Back to sign in") }
    }
}

@Composable
private fun SignedInAccount(
    user: UserProfile,
    isEditing: Boolean,
    isBusy: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    handle: String,
    onHandleChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSignOut: () -> Unit,
    onRequestDeleteAccount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your account", style = MaterialTheme.typography.headlineMedium)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isEditing) {
            OutlinedTextField(name, onNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            OutlinedTextField(handle, onHandleChange, modifier = Modifier.fillMaxWidth(), label = { Text("Handle") }, singleLine = true)
            OutlinedTextField(bio, onBioChange, modifier = Modifier.fillMaxWidth(), label = { Text("Biography") }, minLines = 3, supportingText = { Text("${bio.length} / 500") })
            Button(onClick = onSave, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            TextButton(onClick = onCancel, enabled = !isBusy) { Text("Cancel") }
        } else {
            Text(name.ifBlank { user.name }, style = MaterialTheme.typography.titleLarge)
            if (handle.isNotBlank()) Text("@$handle", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (bio.isNotBlank()) Text(bio, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onEdit, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) { Text("Edit profile") }
        }
        OutlinedButton(onClick = onSignOut, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text("Sign out", modifier = Modifier.padding(start = 8.dp))
        }
        TextButton(
            onClick = onRequestDeleteAccount,
            enabled = !isBusy,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete account")
        }
    }
}
