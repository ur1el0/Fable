package com.example.fable.domain.reading

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressMapperTest {
    @Test
    fun mapsOverallProgressIntoTheSelectedChapter() {
        assertEquals(40, ReaderProgressMapper.chapterProgressPercent(30, 3, 8))
        assertEquals(0, ReaderProgressMapper.chapterProgressPercent(25, 3, 8))
        assertEquals(100, ReaderProgressMapper.chapterProgressPercent(100, 8, 8))
    }

    @Test
    fun clampsInvalidChapterMetadataAndProgress() {
        assertEquals(45, ReaderProgressMapper.chapterProgressPercent(45, 0, 0))
        assertEquals(0, ReaderProgressMapper.chapterProgressPercent(-10, 1, 4))
        assertEquals(100, ReaderProgressMapper.chapterProgressPercent(110, 4, 4))
    }

    @Test
    fun mapsChapterProgressBackToOverallProgress() {
        assertEquals(30, ReaderProgressMapper.overallProgressPercent(40, 3, 8))
        assertEquals(1, ReaderProgressMapper.overallProgressPercent(0, 1, 8))
        assertEquals(100, ReaderProgressMapper.overallProgressPercent(100, 8, 8))
    }

    @Test
    fun restoresThePageContainingTheSavedProgress() {
        assertEquals(0, ReaderProgressMapper.pageIndexForProgress(0, 4))
        assertEquals(0, ReaderProgressMapper.pageIndexForProgress(25, 4))
        assertEquals(1, ReaderProgressMapper.pageIndexForProgress(26, 4))
        assertEquals(3, ReaderProgressMapper.pageIndexForProgress(100, 4))
        assertEquals(0, ReaderProgressMapper.pageIndexForProgress(80, 0))
    }

    @Test
    fun restoresScrollOffsetAndClampsToAvailableRange() {
        assertEquals(400, ReaderProgressMapper.scrollOffsetForProgress(40, 1_000))
        assertEquals(0, ReaderProgressMapper.scrollOffsetForProgress(-10, 1_000))
        assertEquals(1_000, ReaderProgressMapper.scrollOffsetForProgress(110, 1_000))
        assertEquals(0, ReaderProgressMapper.scrollOffsetForProgress(50, -10))
    }
}
