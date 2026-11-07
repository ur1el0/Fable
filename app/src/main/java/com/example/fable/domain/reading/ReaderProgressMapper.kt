package com.example.fable.domain.reading

import kotlin.math.ceil
import kotlin.math.roundToInt

object ReaderProgressMapper {
    fun chapterProgressPercent(
        overallProgressPercent: Int,
        chapterNumber: Int,
        totalChapters: Int
    ): Int {
        val chapterCount = totalChapters.coerceAtLeast(1)
        val currentChapter = chapterNumber.coerceIn(1, chapterCount)
        val overallProgress = overallProgressPercent.coerceIn(0, 100) / 100.0
        return ((overallProgress * chapterCount - (currentChapter - 1)) * 100)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun overallProgressPercent(
        chapterProgressPercent: Int,
        chapterNumber: Int,
        totalChapters: Int
    ): Int {
        val chapterCount = totalChapters.coerceAtLeast(1)
        val currentChapter = chapterNumber.coerceIn(1, chapterCount)
        val chapterProgress = chapterProgressPercent.coerceIn(0, 100) / 100.0
        return (((currentChapter - 1) + chapterProgress) / chapterCount * 100)
            .roundToInt()
            .coerceIn(1, 100)
    }

    fun pageIndexForProgress(chapterProgressPercent: Int, pageCount: Int): Int {
        if (pageCount <= 1) return 0
        val completedPages = ceil(chapterProgressPercent.coerceIn(0, 100) * pageCount / 100.0).toInt()
        return (completedPages - 1).coerceIn(0, pageCount - 1)
    }

    fun scrollOffsetForProgress(chapterProgressPercent: Int, maxScroll: Int): Int {
        val scrollRange = maxScroll.coerceAtLeast(0)
        return (scrollRange * chapterProgressPercent.coerceIn(0, 100) / 100.0)
            .roundToInt()
            .coerceIn(0, scrollRange)
    }
}
