package com.example.fable.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.Story
import com.example.fable.domain.reading.ReaderProgressMapper
import com.example.fable.ui.theme.ReaderPalette
import com.example.fable.ui.theme.scaledFontSize
import com.example.fable.ui.theme.scaledLineHeight
import com.example.fable.ui.theme.toFontFamily
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(FlowPreview::class)
@Composable
fun ContinuousChapterList(
    story: Story,
    chapters: List<Chapter>,
    selectedChapterNumber: Int,
    chapterTotal: Int,
    readerPreferences: ReaderPreferences,
    palette: ReaderPalette,
    listState: LazyListState = rememberLazyListState(),
    onSaveProgress: (Int, Int) -> Unit,
    onEnsureChapterLoaded: (Int) -> Unit,
    onActiveChapterChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedChapters = remember(chapters) { chapters.sortedBy(Chapter::chapterNumber) }
    val latestChapters = rememberUpdatedState(orderedChapters)
    val latestOnSaveProgress = rememberUpdatedState(onSaveProgress)
    val latestOnEnsureChapterLoaded = rememberUpdatedState(onEnsureChapterLoaded)
    val latestOnActiveChapterChanged = rememberUpdatedState(onActiveChapterChanged)
    val initialChapterProgress = remember(story.id, selectedChapterNumber, chapterTotal) {
        if (story.lastReadChapterNumber != null && story.lastReadChapterNumber != selectedChapterNumber) {
            0
        } else {
            ReaderProgressMapper.chapterProgressPercent(
                overallProgressPercent = story.progressPercent,
                chapterNumber = selectedChapterNumber,
                totalChapters = chapterTotal
            )
        }
    }

    LaunchedEffect(listState, story.id, selectedChapterNumber, chapterTotal, orderedChapters.size) {
        val targetIndex = snapshotFlow {
            val currentChapters = latestChapters.value
            if (currentChapters.isEmpty() && story.content.isBlank()) {
                -1
            } else {
                currentChapters.indexOfFirst { it.chapterNumber == selectedChapterNumber }
                    .takeIf { it >= 0 }
                    ?: 0
            }
        }.first { it >= 0 }

        listState.scrollToItem(targetIndex)
        if (initialChapterProgress == 0) return@LaunchedEffect

        val targetReady = snapshotFlow {
            val target = latestChapters.value.getOrNull(targetIndex)
            target?.content?.isNotBlank() == true || target?.pageUrls?.isNotEmpty() == true ||
                ((target == null || target.chapterNumber == 1) && story.content.isNotBlank())
        }.first { it }
        if (!targetReady) return@LaunchedEffect

        val targetItem = snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        }.first { it?.size?.let { size -> size > 0 } == true } ?: return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
        val scrollRange = (targetItem.size - viewportHeight).coerceAtLeast(0)
        val offset = ReaderProgressMapper.scrollOffsetForProgress(initialChapterProgress, scrollRange)
        if (offset > 0) listState.scrollToItem(targetIndex, offset)
    }

    LaunchedEffect(listState, story.id, selectedChapterNumber, orderedChapters.size, chapterTotal) {
        val positions = snapshotFlow {
            currentChapterPosition(listState, latestChapters.value, selectedChapterNumber)
        }
            .filterNotNull()
            .distinctUntilChanged()
        launch {
            positions
                .map { it.chapterNumber }
                .distinctUntilChanged()
                .collect { latestOnActiveChapterChanged.value(it) }
        }
        launch {
            positions
                .drop(1)
                .debounce(PROGRESS_SAVE_DEBOUNCE_MS)
                .collect { position ->
                    val progress = ReaderProgressMapper.overallProgressPercent(
                        chapterProgressPercent = position.progressPercent,
                        chapterNumber = position.chapterNumber,
                        totalChapters = chapterTotal
                    )
                    latestOnSaveProgress.value(progress, position.chapterNumber)
                }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        if (orderedChapters.isEmpty()) {
            item(key = "story-content") {
                val hasContent = story.content.isNotBlank()
                LaunchedEffect(story.id, selectedChapterNumber, hasContent) {
                    if (!hasContent) latestOnEnsureChapterLoaded.value(selectedChapterNumber)
                }
                ChapterContent(
                    story = story,
                    chapter = null,
                    chapterNumber = selectedChapterNumber,
                    content = story.content,
                    readerPreferences = readerPreferences,
                    palette = palette
                )
            }
        }
        itemsIndexed(orderedChapters, key = { _, chapter -> chapter.id }) { _, chapter ->
            val content = chapter.content.takeIf(String::isNotBlank)
                ?: story.content.takeIf { chapter.chapterNumber == 1 && it.isNotBlank() }
                ?: ""
            val hasContent = content.isNotBlank() || chapter.pageUrls.isNotEmpty()
            LaunchedEffect(chapter.id, hasContent) {
                if (!hasContent) latestOnEnsureChapterLoaded.value(chapter.chapterNumber)
            }
            ChapterContent(
                story = story,
                chapter = chapter,
                chapterNumber = chapter.chapterNumber,
                content = content,
                readerPreferences = readerPreferences,
                palette = palette
            )
        }
    }
}

@Composable
private fun ChapterContent(
    story: Story,
    chapter: Chapter?,
    chapterNumber: Int,
    content: String,
    readerPreferences: ReaderPreferences,
    palette: ReaderPalette
) {
    val fontFamily = readerPreferences.font.toFontFamily()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            chapter?.title ?: story.chapter.ifBlank { "Chapter $chapterNumber" },
            fontFamily = fontFamily,
            fontSize = readerPreferences.scaledFontSize(28f).sp,
            lineHeight = readerPreferences.scaledLineHeight(34f, 28f).sp,
            color = palette.foreground
        )
        if (story.contentFormat == ContentFormat.MANGA && chapter?.pageUrls?.isNotEmpty() == true) {
            chapter.pageUrls.forEachIndexed { index, pageUrl ->
                AsyncImage(
                    model = pageUrl,
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }
        } else if (content.isNotBlank()) {
            Text(
                content,
                fontFamily = fontFamily,
                fontSize = readerPreferences.scaledFontSize(18f).sp,
                lineHeight = readerPreferences.scaledLineHeight(31f, 18f).sp,
                color = palette.foreground
            )
        } else {
            Text(
                "This chapter is not available offline yet.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = palette.foreground.copy(alpha = 0.84f),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

private data class ChapterViewportPosition(
    val chapterNumber: Int,
    val progressPercent: Int
)

private fun currentChapterPosition(
    listState: LazyListState,
    chapters: List<Chapter>,
    fallbackChapterNumber: Int
): ChapterViewportPosition? {
    val layoutInfo = listState.layoutInfo
    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val chapterNumber = chapters.getOrNull(firstVisible.index)?.chapterNumber
        ?: fallbackChapterNumber.takeIf { chapters.isEmpty() }
        ?: return null
    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
    val scrollRange = (firstVisible.size - viewportHeight).coerceAtLeast(1)
    val scrolledOffset = (layoutInfo.viewportStartOffset - firstVisible.offset).coerceIn(0, scrollRange)
    val rawProgress = (scrolledOffset * 100f / scrollRange).roundToInt().coerceIn(0, 100)
    return ChapterViewportPosition(chapterNumber, rawProgress / 5 * 5)
}

private const val PROGRESS_SAVE_DEBOUNCE_MS = 900L
