package com.example.fable.data.repository

import com.example.fable.data.local.WriterDraftDao
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.domain.model.WriterDraft
import com.example.fable.domain.repository.WriterDraftRepository

class RoomWriterDraftRepository(
    private val dao: WriterDraftDao
) : WriterDraftRepository {
    override suspend fun get(ownerId: String): WriterDraft? = dao.get(ownerId)?.toDomain()

    override suspend fun save(ownerId: String, draft: WriterDraft) {
        require(ownerId.isNotBlank()) { "A writer draft owner is required." }
        dao.upsert(draft.toEntity(ownerId))
    }

    override suspend fun clear(ownerId: String) {
        dao.clear(ownerId)
    }

    override suspend fun clearIfMatches(ownerId: String, draft: WriterDraft): Boolean =
        dao.clearIfMatches(
            ownerId = ownerId,
            title = draft.title,
            genre = draft.genre,
            chapterTitle = draft.chapterTitle,
            synopsis = draft.synopsis,
            content = draft.content
        ) > 0
}
