package com.example.fable.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val id: String = "",
    val storyId: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String? = "",
    val wordCount: Int = 0,
    val createdAtUtc: String = "",
    val pageUrls: List<String> = emptyList()
)

@Serializable
data class StoryDto(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genre: String = "",
    val chapter: String = "",
    val synopsis: String = "",
    val content: String = "",
    val readTimeMinutes: Int = 0,
    val isBookmarked: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAtUtc: String = "",
    val updatedAtUtc: String = "",
    val coverImageName: String? = null,
    val heroImageName: String? = null,
    val coverImageUrl: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 1,
    val progressPercent: Int = 0,
    val rating: Double? = null,
    val savesCount: String = "0",
    val readsCount: String = "0",
    val isTaleOfTheDay: Boolean = false,
    val isRecentSubmission: Boolean = false,
    val isCuratorSpotlight: Boolean = false,
    val badgeText: String? = null,
    val totalChapters: Int = 0,
    val contentFormat: String = "PROSE",
    val sourceProvider: String = "FABLE_ORIGINAL",
    val providerId: String? = null,
    val providerDownloadCount: Int? = null,
    val chapters: List<ChapterDto>? = null
)

@Serializable
data class CreateStoryRequestDto(
    val title: String,
    val genre: String,
    val synopsis: String,
    val content: String,
    val chapter: String? = null,
    val readTimeMinutes: Int = 0,
    val contentFormat: String = "PROSE"
)

@Serializable
data class GenreDto(
    val id: String = "",
    val name: String = "",
    val storyCount: Int = 0,
    val readersCount: String = "0",
    val description: String = "",
    val imageName: String = "",
    val imageUrl: String? = null
)

@Serializable
data class WriterDto(
    val id: String = "",
    val name: String = "",
    val avatarImageName: String = "",
    val avatarImageUrl: String? = null,
    val storyCount: Int = 0,
    val rating: Double? = null
)

@Serializable
data class UpdateFeedDto(
    val taleOfTheDay: StoryDto? = null,
    val curatorSpotlight: StoryDto? = null,
    val recentSubmissions: List<StoryDto> = emptyList(),
    val totalStories: Int = 0,
    val timestampUtc: String = ""
)

@Serializable
data class ApiErrorDto(val detail: String? = null)
