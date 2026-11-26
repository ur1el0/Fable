package com.example.fable.data.local

import android.content.Context
import com.example.fable.data.remote.ServerUrlValidator
import com.example.fable.domain.repository.ServerConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebugServerConfigStore(
    context: Context,
    override val defaultBaseUrl: String,
    override val isRuntimeSwitchEnabled: Boolean
) : ServerConfigRepository {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _activeBaseUrl = MutableStateFlow(resolveInitialUrl())

    override val activeBaseUrl: StateFlow<String> = _activeBaseUrl.asStateFlow()

    override fun currentBaseUrl(): String =
        if (isRuntimeSwitchEnabled) _activeBaseUrl.value else defaultBaseUrl

    override fun setCustomBaseUrl(rawUrl: String): Result<String> {
        if (!isRuntimeSwitchEnabled) {
            return Result.failure(
                UnsupportedOperationException("Server URL overrides are disabled in release builds.")
            )
        }
        return ServerUrlValidator.validateAndNormalize(rawUrl, allowCleartext = true)
            .onSuccess { normalized ->
                preferences.edit().putString(KEY_OVERRIDE_API_BASE_URL, normalized).apply()
                _activeBaseUrl.value = normalized
            }
    }

    override fun resetToDefault(): String {
        if (isRuntimeSwitchEnabled) {
            preferences.edit().remove(KEY_OVERRIDE_API_BASE_URL).apply()
            _activeBaseUrl.value = defaultBaseUrl
        }
        return defaultBaseUrl
    }

    private fun resolveInitialUrl(): String {
        if (!isRuntimeSwitchEnabled) return defaultBaseUrl
        val stored = preferences.getString(KEY_OVERRIDE_API_BASE_URL, null) ?: return defaultBaseUrl
        return ServerUrlValidator.validateAndNormalize(stored, allowCleartext = true)
            .getOrElse {
                preferences.edit().remove(KEY_OVERRIDE_API_BASE_URL).apply()
                defaultBaseUrl
            }
    }

    private companion object {
        const val PREFS_NAME = "fable_server_config"
        const val KEY_OVERRIDE_API_BASE_URL = "override_api_base_url"
    }
}
