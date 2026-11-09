package com.example.fable.domain.repository

import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile

interface AccountRepository {
    fun hasSession(): Boolean
    fun currentAccountId(): String?
    suspend fun currentUser(): Result<UserProfile>
    suspend fun myStories(): Result<List<Story>>
    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun register(email: String, password: String, name: String, handle: String): Result<UserProfile>
    suspend fun updateProfile(name: String, handle: String, bio: String): Result<UserProfile>
    suspend fun requestPasswordReset(email: String): Result<Unit>
    suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
