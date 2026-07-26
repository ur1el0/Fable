package com.example.fable.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val source: String,
    val description: String?,
    val isFavorite: Boolean = false
)