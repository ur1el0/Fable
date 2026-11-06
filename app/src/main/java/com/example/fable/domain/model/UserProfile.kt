package com.example.fable.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val handle: String,
    val bio: String,
    val avatarImageUrl: String?,
    val createdAtUtc: String
)
