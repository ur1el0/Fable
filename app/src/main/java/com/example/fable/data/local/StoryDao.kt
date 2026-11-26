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

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    fun observeStory(storyId: String): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    suspend fun getStory(storyId: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE id IN (:storyIds)")
    suspend fun getStories(storyIds: List<String>): List<StoryEntity>

    @Upsert
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM shelf_states WHERE ownerId = :ownerId")
    fun observeShelfStates(ownerId: String): Flow<List<ShelfStateEntity>>

    @Query("SELECT * FROM shelf_states WHERE ownerId = :ownerId")
    suspend fun getShelfStates(ownerId: String): List<ShelfStateEntity>

    @Query("SELECT * FROM shelf_states WHERE ownerId = :ownerId AND storyId = :storyId LIMIT 1")
    suspend fun getShelfState(ownerId: String, storyId: String): ShelfStateEntity?

    @Upsert
    suspend fun upsertShelfState(state: ShelfStateEntity)

    @Upsert
    suspend fun upsertShelfStates(states: List<ShelfStateEntity>)

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

    @Query(
        "SELECT DISTINCT storyId FROM chapters " +
            "WHERE TRIM(content) != '' OR (pageUrls != '[]' AND TRIM(pageUrls) != '') " +
            "UNION " +
            "SELECT id FROM stories WHERE TRIM(content) != ''"
    )
    fun observeDownloadedStoryIds(): Flow<List<String>>

    @Query("UPDATE stories SET content = '' WHERE id = :storyId")
    suspend fun clearStoryContent(storyId: String)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStory(storyId: String)

    @Query("DELETE FROM stories WHERE ownerUserId = :ownerId")
    suspend fun deleteStoriesByOwner(ownerId: String)

    @Query("DELETE FROM shelf_states WHERE storyId = :storyId")
    suspend fun deleteShelfStatesForStory(storyId: String)

    @Query("DELETE FROM shelf_states WHERE ownerId = :ownerId")
    suspend fun deleteShelfStatesForOwner(ownerId: String)

    @Transaction
    suspend fun clearOfflineCache(storyId: String) {
        deleteChapters(storyId)
        clearStoryContent(storyId)
    }

    @Transaction
    suspend fun deleteStoryAndShelfStates(storyId: String) {
        deleteShelfStatesForStory(storyId)
        deleteStory(storyId)
    }

    @Transaction
    suspend fun purgeAccountData(ownerId: String) {
        deleteShelfStatesForOwner(ownerId)
        deleteStoriesByOwner(ownerId)
    }
}
