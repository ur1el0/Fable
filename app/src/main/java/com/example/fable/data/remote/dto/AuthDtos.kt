package com.example.fable.data.remote.dto

import com.example.fable.domain.model.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val avatarImageName: String? = null,
    val avatarImageUrl: String? = null,
    val createdAtUtc: String = ""
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val tokenType: String = "bearer",
    val user: UserDto
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String,
    val handle: String = ""
)

@Serializable
data class ProfileUpdateRequestDto(
    val name: String,
    val handle: String = "",
    val bio: String = ""
)

@Serializable
data class PasswordResetRequestDto(val email: String)

@Serializable
data class PasswordResetConfirmRequestDto(
    val token: String,
    val newPassword: String
)

fun UserDto.toDomain() = UserProfile(
    id = id,
    email = email,
    name = name,
    handle = handle,
    bio = bio,
    avatarImageUrl = avatarImageUrl,
    createdAtUtc = createdAtUtc
)
