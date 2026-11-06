package com.example.fable.domain.model

enum class ContentFormat(val wireValue: String) {
    PROSE("PROSE"),
    MANGA("MANGA");

    companion object {
        fun fromWireValue(value: String): ContentFormat =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: PROSE
    }
}

enum class SourceProvider(val wireValue: String) {
    GUTENBERG("GUTENBERG"),
    STANDARD_EBOOKS("STANDARD_EBOOKS"),
    MANGADEX("MANGADEX"),
    FABLE_ORIGINAL("FABLE_ORIGINAL");

    companion object {
        fun fromWireValue(value: String): SourceProvider =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: FABLE_ORIGINAL
    }
}

data class Chapter(
    val id: String,
    val storyId: String,
    val chapterNumber: Int,
    val title: String,
    val content: String = "",
    val wordCount: Int = 0,
    val createdAtUtc: String = "",
    val pageUrls: List<String> = emptyList()
)

data class Story(
    val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val synopsis: String,
    val content: String,
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
    val contentFormat: ContentFormat = ContentFormat.PROSE,
    val sourceProvider: SourceProvider = SourceProvider.FABLE_ORIGINAL,
    val providerId: String? = null,
    val providerDownloadCount: Int? = null,
    val chapters: List<Chapter> = emptyList(),
    val lastReadChapterId: String? = null,
    val lastReadChapterNumber: Int? = null
)

data class GenreCategory(
    val id: String,
    val name: String,
    val storyCount: Int = 0,
    val readersCount: String = "0",
    val description: String = "",
    val imageName: String = "",
    val imageUrl: String? = null
)

data class Writer(
    val id: String,
    val name: String,
    val avatarImageName: String = "",
    val avatarImageUrl: String? = null,
    val storyCount: Int = 0,
    val rating: Double? = null
)

data class UpdateFeed(
    val taleOfTheDay: Story? = null,
    val curatorSpotlight: Story? = null,
    val recentSubmissions: List<Story> = emptyList(),
    val totalStories: Int = 0,
    val timestampUtc: String = ""
)
