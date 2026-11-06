package com.example.fable.data.local

import com.example.fable.data.remote.dto.ChapterDto
import com.example.fable.data.remote.dto.StoryDto
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story

fun StoryEntity.toDomain() = Story(
    id = id,
    title = title,
    author = author,
    genre = genre,
    chapter = chapter,
    synopsis = synopsis,
    content = content,
    readTimeMinutes = readTimeMinutes,
    isBookmarked = isBookmarked,
    isCompleted = isCompleted,
    createdAtUtc = createdAtUtc,
    updatedAtUtc = updatedAtUtc,
    coverImageName = coverImageName,
    heroImageName = heroImageName,
    coverImageUrl = coverImageUrl,
    totalPages = totalPages,
    currentPage = currentPage,
    progressPercent = progressPercent,
    rating = rating,
    savesCount = savesCount,
    readsCount = readsCount,
    isTaleOfTheDay = isTaleOfTheDay,
    isRecentSubmission = isRecentSubmission,
    isCuratorSpotlight = isCuratorSpotlight,
    badgeText = badgeText,
    totalChapters = totalChapters,
    contentFormat = ContentFormat.fromWireValue(contentFormat),
    sourceProvider = SourceProvider.fromWireValue(sourceProvider),
    providerId = providerId,
    providerDownloadCount = providerDownloadCount,
    lastReadChapterId = lastReadChapterId,
    lastReadChapterNumber = lastReadChapterNumber
)

fun StoryDto.toEntity(previous: StoryEntity? = null): StoryEntity = StoryEntity(
    id = id,
    title = title,
    author = author,
    genre = genre,
    chapter = chapter,
    synopsis = synopsis,
    content = content,
    readTimeMinutes = readTimeMinutes,
    createdAtUtc = createdAtUtc,
    updatedAtUtc = updatedAtUtc,
    coverImageName = coverImageName,
    heroImageName = heroImageName,
    coverImageUrl = coverImageUrl,
    totalPages = totalPages,
    currentPage = previous?.currentPage ?: currentPage,
    progressPercent = previous?.progressPercent ?: progressPercent,
    rating = rating,
    savesCount = savesCount,
    readsCount = readsCount,
    isTaleOfTheDay = isTaleOfTheDay,
    isRecentSubmission = isRecentSubmission,
    isCuratorSpotlight = isCuratorSpotlight,
    badgeText = badgeText,
    totalChapters = totalChapters,
    contentFormat = contentFormat,
    sourceProvider = sourceProvider,
    providerId = providerId,
    providerDownloadCount = providerDownloadCount,
    isBookmarked = previous?.isBookmarked ?: isBookmarked,
    isCompleted = previous?.isCompleted ?: isCompleted,
    lastReadChapterId = previous?.lastReadChapterId,
    lastReadChapterNumber = previous?.lastReadChapterNumber,
    shelfUpdatedAtUtc = previous?.shelfUpdatedAtUtc ?: updatedAtUtc
)

fun ChapterDto.toEntity() = ChapterEntity(
    id = id,
    storyId = storyId,
    chapterNumber = chapterNumber,
    title = title,
    content = content.orEmpty(),
    wordCount = wordCount,
    createdAtUtc = createdAtUtc,
    pageUrls = pageUrls
)

fun Chapter.toEntity() = ChapterEntity(
    id = id,
    storyId = storyId,
    chapterNumber = chapterNumber,
    title = title,
    content = content,
    wordCount = wordCount,
    createdAtUtc = createdAtUtc,
    pageUrls = pageUrls
)
