package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fable.domain.model.WriterDraft

@Entity(tableName = "writer_drafts")
data class WriterDraftEntity(
    @PrimaryKey val ownerId: String,
    val title: String,
    val genre: String,
    val chapterTitle: String,
    val synopsis: String,
    val content: String
)

fun WriterDraftEntity.toDomain() = WriterDraft(
    title = title,
    genre = genre,
    chapterTitle = chapterTitle,
    synopsis = synopsis,
    content = content
)

fun WriterDraft.toEntity(ownerId: String) = WriterDraftEntity(
    ownerId = ownerId,
    title = title,
    genre = genre,
    chapterTitle = chapterTitle,
    synopsis = synopsis,
    content = content
)
