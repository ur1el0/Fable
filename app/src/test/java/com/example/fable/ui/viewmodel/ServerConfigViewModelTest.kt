package com.example.fable.ui.viewmodel

import com.example.fable.data.remote.ServerUrlValidator
import com.example.fable.data.remote.dto.HealthDto
import com.example.fable.domain.repository.ServerConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerConfigViewModelTest {
    @Test
    fun validUrlTriggersOnChangedAndInvalidUrlPreservesActiveUrl() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeServerConfigRepository(enabled = true)
            val viewModel = ServerConfigViewModel(repo) { Result.success(HealthDto("healthy", "connected")) }
            var changedCount = 0

            viewModel.applyBaseUrl("https://fable-api-cbo8.onrender.com") { changedCount += 1 }
            assertEquals(1, changedCount)
            assertEquals("https://fable-api-cbo8.onrender.com/api/v1/", viewModel.uiState.value.activeBaseUrl)
            assertNull(viewModel.uiState.value.errorMessage)

            viewModel.applyBaseUrl("https://fable.test/wrong-path") { changedCount += 1 }
            assertEquals(1, changedCount)
            assertEquals("https://fable-api-cbo8.onrender.com/api/v1/", viewModel.uiState.value.activeBaseUrl)
            assertEquals("Server URL path must end with /api/v1/.", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun testConnectionUpdatesHealthStatusAndReleaseModeDisablesSwitch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val releaseRepo = FakeServerConfigRepository(enabled = false)
            val releaseVm = ServerConfigViewModel(releaseRepo) {
                Result.success(HealthDto("healthy", "connected"))
            }
            assertFalse(releaseVm.uiState.value.isRuntimeSwitchEnabled)

            releaseVm.applyBaseUrl("http://10.0.2.2:8001/api/v1/")
            assertTrue(releaseVm.uiState.value.errorMessage!!.contains("disabled in release builds"))

            releaseVm.testConnection()
            advanceUntilIdle()
            assertEquals("Connected (healthy, database connected).", releaseVm.uiState.value.healthStatusMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeServerConfigRepository(
        private val enabled: Boolean
    ) : ServerConfigRepository {
        override val isRuntimeSwitchEnabled: Boolean = enabled
        override val defaultBaseUrl: String = ServerUrlValidator.EMULATOR_LOCAL_BASE_URL
        private val _activeBaseUrl = MutableStateFlow(defaultBaseUrl)
        override val activeBaseUrl: StateFlow<String> = _activeBaseUrl

        override fun currentBaseUrl(): String = _activeBaseUrl.value

        override fun setCustomBaseUrl(rawUrl: String): Result<String> {
            if (!enabled) {
                return Result.failure(UnsupportedOperationException("Server URL overrides are disabled in release builds."))
            }
            return ServerUrlValidator.validateAndNormalize(rawUrl, allowCleartext = true)
                .onSuccess { _activeBaseUrl.value = it }
        }

        override fun resetToDefault(): String {
            _activeBaseUrl.value = defaultBaseUrl
            return defaultBaseUrl
        }
    }
}
