package com.example.fable.data.repository

import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.toEntity
import com.example.fable.data.local.toSyncDto
import com.example.fable.data.remote.dto.ShelfSyncItemDto
import java.time.Instant
import java.time.OffsetDateTime

internal data class ShelfSyncPlan(
    val serverWins: List<ShelfStateEntity>,
    val upload: List<ShelfSyncItemDto>
)

internal fun planShelfSync(
    ownerId: String,
    localStates: List<ShelfStateEntity>,
    remoteItems: List<ShelfSyncItemDto>
): ShelfSyncPlan {
    val localByStory = localStates.associateBy(ShelfStateEntity::storyId)
    val remoteByStory = remoteItems.associateBy(ShelfSyncItemDto::storyId)
    val serverWins = mutableListOf<ShelfStateEntity>()
    val upload = mutableListOf<ShelfSyncItemDto>()

    remoteByStory.values.forEach { remote ->
        val local = localByStory[remote.storyId]
        if (local == null || !isNewer(local.updatedAtUtc, remote.updatedAtUtc)) {
            serverWins += remote.toEntity(ownerId, previous = local)
        } else {
            upload += local.toSyncDto()
        }
    }

    localStates.filterNot { it.storyId in remoteByStory }.forEach { upload += it.toSyncDto() }
    return ShelfSyncPlan(serverWins = serverWins, upload = upload)
}

internal fun isNewer(localTimestamp: String, remoteTimestamp: String): Boolean {
    val localInstant = parseTimestamp(localTimestamp) ?: return false
    val remoteInstant = parseTimestamp(remoteTimestamp) ?: return true
    return localInstant > remoteInstant
}

private fun parseTimestamp(timestamp: String): Instant? = runCatching {
    try {
        Instant.parse(timestamp)
    } catch (_: Exception) {
        OffsetDateTime.parse(timestamp).toInstant()
    }
}.getOrNull()
