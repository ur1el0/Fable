package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.data.remote.dto.HealthDto
import com.example.fable.domain.repository.ServerConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerConfigUiState(
    val isRuntimeSwitchEnabled: Boolean = false,
    val defaultBaseUrl: String = "",
    val activeBaseUrl: String = "",
    val isCheckingHealth: Boolean = false,
    val healthStatusMessage: String? = null,
    val errorMessage: String? = null
)

class ServerConfigViewModel(
    private val serverConfigRepository: ServerConfigRepository,
    private val checkHealth: suspend () -> Result<HealthDto>
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ServerConfigUiState(
            isRuntimeSwitchEnabled = serverConfigRepository.isRuntimeSwitchEnabled,
            defaultBaseUrl = serverConfigRepository.defaultBaseUrl,
            activeBaseUrl = serverConfigRepository.currentBaseUrl()
        )
    )
    val uiState: StateFlow<ServerConfigUiState> = mutableUiState.asStateFlow()

    init {
        serverConfigRepository.activeBaseUrl
            .onEach { url -> mutableUiState.update { it.copy(activeBaseUrl = url) } }
            .launchIn(viewModelScope)
    }

    fun applyBaseUrl(rawUrl: String, onChanged: () -> Unit = {}) {
        serverConfigRepository.setCustomBaseUrl(rawUrl).fold(
            onSuccess = { normalized ->
                mutableUiState.update {
                    it.copy(
                        activeBaseUrl = normalized,
                        healthStatusMessage = "Switched server to $normalized",
                        errorMessage = null
                    )
                }
                onChanged()
            },
            onFailure = { error ->
                mutableUiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Server URL could not be updated.",
                        healthStatusMessage = null
                    )
                }
            }
        )
    }

    fun resetToDefault(onChanged: () -> Unit = {}) {
        val defaultUrl = serverConfigRepository.resetToDefault()
        mutableUiState.update {
            it.copy(
                activeBaseUrl = defaultUrl,
                healthStatusMessage = "Reset to default server.",
                errorMessage = null
            )
        }
        onChanged()
    }

    fun testConnection() {
        if (mutableUiState.value.isCheckingHealth) return
        mutableUiState.update {
            it.copy(isCheckingHealth = true, healthStatusMessage = null, errorMessage = null)
        }
        viewModelScope.launch {
            checkHealth().fold(
                onSuccess = { health ->
                    mutableUiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatusMessage = "Connected (${health.status}, database ${health.database}).",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatusMessage = null,
                            errorMessage = error.message ?: "Could not reach server."
                        )
                    }
                }
            )
        }
    }

    class Factory(
        private val serverConfigRepository: ServerConfigRepository,
        private val checkHealth: suspend () -> Result<HealthDto>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ServerConfigViewModel::class.java))
            return ServerConfigViewModel(serverConfigRepository, checkHealth) as T
        }
    }
}
