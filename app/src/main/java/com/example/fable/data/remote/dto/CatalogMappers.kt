package com.example.fable.data.remote.dto

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer

fun ChapterDto.toDomain() = Chapter(
    id = id,
    storyId = storyId,
    chapterNumber = chapterNumber,
    title = title,
    content = content.orEmpty(),
    wordCount = wordCount,
    createdAtUtc = createdAtUtc,
    pageUrls = pageUrls
)

fun StoryDto.toDomain() = Story(
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
    chapters = chapters.orEmpty().map(ChapterDto::toDomain)
)

fun GenreDto.toDomain() = GenreCategory(
    id = id,
    name = name,
    storyCount = storyCount,
    readersCount = readersCount,
    description = description,
    imageName = imageName,
    imageUrl = imageUrl
)

fun WriterDto.toDomain() = Writer(
    id = id,
    name = name,
    avatarImageName = avatarImageName,
    avatarImageUrl = avatarImageUrl,
    storyCount = storyCount,
    rating = rating
)

fun UpdateFeedDto.toDomain() = UpdateFeed(
    taleOfTheDay = taleOfTheDay?.toDomain(),
    curatorSpotlight = curatorSpotlight?.toDomain(),
    recentSubmissions = recentSubmissions.map(StoryDto::toDomain),
    totalStories = totalStories,
    timestampUtc = timestampUtc
)
