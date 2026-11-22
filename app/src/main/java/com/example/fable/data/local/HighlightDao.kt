package com.example.fable.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Query(
        "SELECT * FROM story_highlights " +
            "WHERE ownerId = :ownerId AND storyId = :storyId " +
            "ORDER BY chapterNumber ASC, paragraphIndex ASC, startOffset ASC"
    )
    fun observeHighlights(ownerId: String, storyId: String): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM story_highlights WHERE ownerId = :ownerId AND id = :highlightId")
    suspend fun deleteHighlight(ownerId: String, highlightId: String)

    @Query("DELETE FROM story_highlights WHERE ownerId = :ownerId")
    suspend fun deleteHighlightsForOwner(ownerId: String)

    @Query("DELETE FROM story_highlights WHERE storyId = :storyId")
    suspend fun deleteHighlightsForStory(storyId: String)
}
