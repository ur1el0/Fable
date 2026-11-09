package com.example.fable.data.repository

import com.example.fable.data.local.SecureSessionStore
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.LoginRequestDto
import com.example.fable.data.remote.dto.PasswordResetConfirmRequestDto
import com.example.fable.data.remote.dto.PasswordResetRequestDto
import com.example.fable.data.remote.dto.ProfileUpdateRequestDto
import com.example.fable.data.remote.dto.RegisterRequestDto
import com.example.fable.data.remote.dto.toDomain
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile
import com.example.fable.domain.repository.AccountRepository

class RemoteAccountRepository(
    private val api: FableApiService,
    private val sessionStore: SecureSessionStore
) : AccountRepository {
    override fun hasSession(): Boolean = !sessionStore.readAccessToken().isNullOrBlank()

    override fun currentAccountId(): String? = sessionStore.readAccountId()

    override suspend fun currentUser(): Result<UserProfile> = runCatching {
        api.fetchCurrentUser().toDomain()
    }

    override suspend fun myStories(): Result<List<Story>> = runCatching {
        api.fetchMyStories().map { it.toDomain() }
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> = runCatching {
        val response = api.login(LoginRequestDto(email.trim(), password))
        sessionStore.writeSession(response.accessToken, response.user.id)
        response.user.toDomain()
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        handle: String
    ): Result<UserProfile> = runCatching {
        val response = api.register(RegisterRequestDto(email.trim(), password, name.trim(), handle.trim()))
        sessionStore.writeSession(response.accessToken, response.user.id)
        response.user.toDomain()
    }

    override suspend fun updateProfile(name: String, handle: String, bio: String): Result<UserProfile> = runCatching {
        api.updateProfile(ProfileUpdateRequestDto(name.trim(), handle.trim(), bio.trim())).toDomain()
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        api.requestPasswordReset(PasswordResetRequestDto(email.trim()))
        Unit
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> = runCatching {
        api.confirmPasswordReset(PasswordResetConfirmRequestDto(token.trim(), newPassword))
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        val remoteResult = runCatching { api.logout() }
        sessionStore.clear()
        remoteResult.getOrThrow()
    }
}
