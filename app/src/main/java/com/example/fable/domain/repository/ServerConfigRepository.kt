package com.example.fable.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ServerConfigRepository {
    val isRuntimeSwitchEnabled: Boolean
    val defaultBaseUrl: String
    val activeBaseUrl: StateFlow<String>
    fun currentBaseUrl(): String
    fun setCustomBaseUrl(rawUrl: String): Result<String>
    fun resetToDefault(): String
}
