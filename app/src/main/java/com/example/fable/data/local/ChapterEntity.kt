package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.fable.domain.model.Chapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId"), Index(value = ["storyId", "chapterNumber"], unique = true)]
)
@TypeConverters(FableTypeConverters::class)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val chapterNumber: Int,
    val title: String,
    val content: String,
    val wordCount: Int,
    val createdAtUtc: String,
    val pageUrls: List<String>
)

class FableTypeConverters {
    @TypeConverter
    fun encodeStringList(values: List<String>): String = Json.encodeToString(values)

    @TypeConverter
    fun decodeStringList(value: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}

fun ChapterEntity.toDomain() = Chapter(
    id = id,
    storyId = storyId,
    chapterNumber = chapterNumber,
    title = title,
    content = content,
    wordCount = wordCount,
    createdAtUtc = createdAtUtc,
    pageUrls = pageUrls
)
