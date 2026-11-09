package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile
import com.example.fable.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: UserProfile? = null,
    val localAccountId: String? = null,
    val publishedStories: List<Story> = emptyList(),
    val isLoadingPublishedStories: Boolean = false,
    val publishedStoriesErrorMessage: String? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null
)

class AccountViewModel(
    private val repository: AccountRepository
) : ViewModel() {
    private val hasStoredSession = repository.hasSession()
    private val mutableUiState = MutableStateFlow(
        AccountUiState(localAccountId = repository.currentAccountId())
    )
    val uiState: StateFlow<AccountUiState> = mutableUiState.asStateFlow()

    init {
        if (hasStoredSession) refreshProfile()
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            showError("Enter your email and password.")
            return
        }
        viewModelScope.launch {
            beginRequest()
            repository.signIn(email, password).fold(
                onSuccess = { user ->
                    mutableUiState.update {
                        it.copy(
                            user = user,
                            localAccountId = user.id,
                            isBusy = false,
                            errorMessage = null,
                            noticeMessage = null
                        )
                    }
                    loadPublishedStories(user.id)
                },
                onFailure = ::finishWithError
            )
        }
    }

    fun register(email: String, password: String, name: String, handle: String) {
        when {
            email.isBlank() || name.isBlank() -> showError("Enter your name and email.")
            password.length < 12 -> showError("Password must be at least 12 characters.")
            else -> viewModelScope.launch {
                beginRequest()
                repository.register(email, password, name, handle).fold(
                    onSuccess = { user ->
                        mutableUiState.update {
                            it.copy(
                                user = user,
                                localAccountId = user.id,
                                isBusy = false,
                                errorMessage = null,
                                noticeMessage = null
                            )
                        }
                        loadPublishedStories(user.id)
                    },
                    onFailure = ::finishWithError
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            beginRequest()
            repository.currentUser().fold(
                onSuccess = { user ->
                    mutableUiState.update {
                        it.copy(user = user, localAccountId = user.id, isBusy = false, errorMessage = null)
                    }
                    loadPublishedStories(user.id)
                },
                onFailure = ::finishWithError
            )
        }
    }

    fun saveProfile(name: String, handle: String, bio: String) {
        if (name.isBlank()) {
            showError("Name cannot be empty.")
            return
        }
        if (bio.length > 500) {
            showError("Biography must be 500 characters or fewer.")
            return
        }
        viewModelScope.launch {
            beginRequest()
            repository.updateProfile(name, handle, bio).fold(
                onSuccess = { user -> mutableUiState.update { it.copy(user = user, isBusy = false, errorMessage = null, noticeMessage = "Profile saved.") } },
                onFailure = ::finishWithError
            )
        }
    }

    fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            showError("Enter your account email.")
            return
        }
        viewModelScope.launch {
            beginRequest()
            repository.requestPasswordReset(email).fold(
                onSuccess = { mutableUiState.update { it.copy(isBusy = false, errorMessage = null, noticeMessage = "If an account uses that address, recovery instructions will arrive shortly.") } },
                onFailure = ::finishWithError
            )
        }
    }

    fun confirmPasswordReset(token: String, newPassword: String) {
        if (token.isBlank() || newPassword.length < 12) {
            showError("Enter a valid reset token and a password of at least 12 characters.")
            return
        }
        viewModelScope.launch {
            beginRequest()
            repository.confirmPasswordReset(token, newPassword).fold(
                onSuccess = { mutableUiState.update { it.copy(isBusy = false, errorMessage = null, noticeMessage = "Password updated. Sign in with your new password.") } },
                onFailure = ::finishWithError
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            beginRequest()
            repository.signOut().fold(
                onSuccess = { mutableUiState.value = AccountUiState(noticeMessage = "Signed out.") },
                onFailure = { error ->
                    mutableUiState.value = AccountUiState(
                        errorMessage = error.message ?: "Signed out locally; the server could not revoke this session."
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        mutableUiState.update { it.copy(errorMessage = null, noticeMessage = null) }
    }

    private fun loadPublishedStories(ownerId: String) {
        if (mutableUiState.value.user?.id != ownerId) return
        mutableUiState.update {
            it.copy(
                publishedStories = emptyList(),
                isLoadingPublishedStories = true,
                publishedStoriesErrorMessage = null
            )
        }
        viewModelScope.launch {
            repository.myStories().fold(
                onSuccess = { stories ->
                    mutableUiState.update { state ->
                        if (state.user?.id == ownerId) {
                            state.copy(
                                publishedStories = stories,
                                isLoadingPublishedStories = false,
                                publishedStoriesErrorMessage = null
                            )
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    mutableUiState.update { state ->
                        if (state.user?.id == ownerId) {
                            state.copy(
                                isLoadingPublishedStories = false,
                                publishedStoriesErrorMessage = error.message
                            )
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }

    private fun beginRequest() {
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, noticeMessage = null) }
    }

    private fun showError(message: String) {
        mutableUiState.update { it.copy(errorMessage = message, noticeMessage = null) }
    }

    private fun finishWithError(error: Throwable) {
        mutableUiState.update { it.copy(isBusy = false, errorMessage = error.message ?: "The request could not be completed.") }
    }

    class Factory(private val repository: AccountRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AccountViewModel::class.java))
            return AccountViewModel(repository) as T
        }
    }
}
