package com.example.fable.ui.screens

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile
import com.example.fable.ui.components.StoryCover
import com.example.fable.ui.viewmodel.AccountUiState

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
    modifier: Modifier = Modifier
) {
    var isRegistration by rememberSaveable { mutableStateOf(false) }
    var isRecoveringPassword by rememberSaveable { mutableStateOf(false) }
    var isEditingProfile by rememberSaveable(state.user?.id) { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.name.orEmpty()) }
    var handle by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.handle.orEmpty()) }
    var bio by rememberSaveable(state.user?.id) { mutableStateOf(state.user?.bio.orEmpty()) }
    var resetToken by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
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
                        onSignOut = onSignOut
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
                        PublishedStoryRow(story, onClick = { onStorySelected(story) })
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
        }
    }
}

@Composable
private fun PublishedStoryRow(story: Story, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StoryCover(story, Modifier.size(width = 54.dp, height = 74.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(story.genre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(story.synopsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    onSignOut: () -> Unit
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
    }
}
