package com.example.fable.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY updatedAtUtc DESC, title COLLATE NOCASE")
    fun observeCatalog(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isBookmarked = 1 OR progressPercent > 0 OR isCompleted = 1 ORDER BY shelfUpdatedAtUtc DESC")
    fun observeShelf(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    fun observeStory(storyId: String): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    suspend fun getStory(storyId: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE id IN (:storyIds)")
    suspend fun getStories(storyIds: List<String>): List<StoryEntity>

    @Upsert
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM chapters WHERE storyId = :storyId ORDER BY chapterNumber")
    fun observeChapters(storyId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE storyId = :storyId AND chapterNumber = :chapterNumber LIMIT 1")
    suspend fun getChapter(storyId: String, chapterNumber: Int): ChapterEntity?

    @Query("DELETE FROM chapters WHERE storyId = :storyId")
    suspend fun deleteChapters(storyId: String)

    @Upsert
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Transaction
    suspend fun replaceChapters(storyId: String, chapters: List<ChapterEntity>) {
        deleteChapters(storyId)
        upsertChapters(chapters)
    }

    @Query("UPDATE stories SET isBookmarked = :isBookmarked, isCompleted = :isCompleted, progressPercent = :progressPercent, currentPage = :currentPage, lastReadChapterId = :chapterId, lastReadChapterNumber = :chapterNumber, shelfUpdatedAtUtc = :updatedAtUtc WHERE id = :storyId")
    suspend fun updateShelfState(
        storyId: String,
        isBookmarked: Boolean,
        isCompleted: Boolean,
        progressPercent: Int,
        currentPage: Int,
        chapterId: String?,
        chapterNumber: Int?,
        updatedAtUtc: String
    )
}
