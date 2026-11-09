package com.example.fable.domain.repository

import com.example.fable.domain.model.WriterDraft

interface WriterDraftRepository {
    suspend fun get(ownerId: String): WriterDraft?
    suspend fun save(ownerId: String, draft: WriterDraft)
    suspend fun clear(ownerId: String)
    suspend fun clearIfMatches(ownerId: String, draft: WriterDraft): Boolean
}
