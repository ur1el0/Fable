package com.example.fable.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WriterDraftDao {
    @Query("SELECT * FROM writer_drafts WHERE ownerId = :ownerId LIMIT 1")
    suspend fun get(ownerId: String): WriterDraftEntity?

    @Upsert
    suspend fun upsert(draft: WriterDraftEntity)

    @Query("DELETE FROM writer_drafts WHERE ownerId = :ownerId")
    suspend fun clear(ownerId: String)

    @Query(
        "DELETE FROM writer_drafts WHERE ownerId = :ownerId AND title = :title " +
            "AND genre = :genre AND chapterTitle = :chapterTitle AND synopsis = :synopsis " +
            "AND content = :content"
    )
    suspend fun clearIfMatches(
        ownerId: String,
        title: String,
        genre: String,
        chapterTitle: String,
        synopsis: String,
        content: String
    ): Int
}
