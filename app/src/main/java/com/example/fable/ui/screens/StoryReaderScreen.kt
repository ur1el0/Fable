package com.example.fable.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.ReaderLayout
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.Story
import com.example.fable.domain.reading.ReaderPagePacker
import com.example.fable.domain.reading.ReaderProgressMapper
import com.example.fable.ui.theme.readerPalette
import com.example.fable.ui.theme.scaledFontSize
import com.example.fable.ui.theme.scaledLineHeight
import com.example.fable.ui.theme.toFontFamily
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@OptIn(FlowPreview::class)
@Composable
fun StoryReaderScreen(
    story: Story,
    chapters: List<Chapter>,
    chapterNumber: Int,
    errorMessage: String?,
    readerPreferences: ReaderPreferences,
    onBack: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onSaveProgress: (Int, Int) -> Unit,
    onReadingSessionEnded: (String, Int, Boolean) -> Unit,
    onReaderPreferencesChanged: (ReaderPreferences) -> Unit,
    onResetReaderPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestStory by rememberUpdatedState(story)
    val latestOnReadingSessionEnded by rememberUpdatedState(onReadingSessionEnded)
    var sessionStartedAt by remember(story.id) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var isSessionActive by remember(story.id) { mutableStateOf(true) }
    var isChromeVisible by rememberSaveable(story.id) { mutableStateOf(true) }
    var isDisplayOptionsOpen by rememberSaveable { mutableStateOf(false) }
    val chapterIndex = chapters.indexOfFirst { it.chapterNumber == chapterNumber }
    val chapter = chapters.getOrNull(chapterIndex)
    val chapterTotal = maxOf(story.totalChapters, chapters.size, 1)
    val content = chapter?.content?.takeIf(String::isNotBlank) ?: story.content
    val palette = readerPalette(readerPreferences.theme)

    DisposableEffect(lifecycleOwner, story.id) {
        fun endActiveSegment() {
            val elapsedSeconds = ((SystemClock.elapsedRealtime() - sessionStartedAt) / 1_000L).toInt()
            latestOnReadingSessionEnded(latestStory.id, elapsedSeconds, latestStory.isCompleted)
            isSessionActive = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (isSessionActive) endActiveSegment()
                Lifecycle.Event.ON_START -> if (!isSessionActive) {
                    sessionStartedAt = SystemClock.elapsedRealtime()
                    isSessionActive = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (isSessionActive) endActiveSegment()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier.fillMaxSize().background(palette.background)) {
        key(story.id, chapterNumber) {
            ReaderChapterContent(
                story = story,
                chapter = chapter,
                chapterNumber = chapterNumber,
                chapterTotal = chapterTotal,
                content = content,
                readerPreferences = readerPreferences,
                palette = palette,
                onSaveProgress = onSaveProgress,
                onToggleChrome = { isChromeVisible = !isChromeVisible },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (errorMessage == null) 64.dp else 104.dp, bottom = 80.dp)
            )
        }
        ReaderControlsOverlay(
            visible = isChromeVisible,
            story = story,
            chapter = chapter,
            chapterNumber = chapterNumber,
            errorMessage = errorMessage,
            palette = palette,
            canGoToPreviousChapter = chapterIndex > 0,
            hasNextChapter = chapterIndex in 0 until chapters.lastIndex,
            onBack = onBack,
            onDisplayOptions = { isDisplayOptionsOpen = true },
            onPreviousChapter = {
                chapters.getOrNull(chapterIndex - 1)?.let { onSelectChapter(it.chapterNumber) }
            },
            onNextChapter = {
                chapters.getOrNull(chapterIndex + 1)?.let { onSelectChapter(it.chapterNumber) }
            },
            onFinish = { onSaveProgress(100, chapterNumber) }
        )
    }

    if (isDisplayOptionsOpen) {
        ReaderDisplayOptionsSheet(
            preferences = readerPreferences,
            showLayoutOptions = story.contentFormat != ContentFormat.MANGA,
            onPreferencesChanged = onReaderPreferencesChanged,
            onReset = onResetReaderPreferences,
            onDismiss = { isDisplayOptionsOpen = false }
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ReaderChapterContent(
    story: Story,
    chapter: Chapter?,
    chapterNumber: Int,
    chapterTotal: Int,
    content: String,
    readerPreferences: ReaderPreferences,
    palette: com.example.fable.ui.theme.ReaderPalette,
    onSaveProgress: (Int, Int) -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paginated = readerPreferences.layout == ReaderLayout.PAGINATED &&
        story.contentFormat != ContentFormat.MANGA
    val pages = remember(content, readerPreferences.fontScalePercent, paginated) {
        if (paginated) {
            ReaderPagePacker.chunkIntoPages(content, readerPreferences.fontScalePercent)
                .ifEmpty { listOf("This chapter is not available offline yet.") }
        } else {
            emptyList()
        }
    }
    val fontFamily = readerPreferences.font.toFontFamily()
    val restoredChapterProgress = remember(story.id, chapterNumber, chapterTotal) {
        ReaderProgressMapper.chapterProgressPercent(
            overallProgressPercent = story.progressPercent,
            chapterNumber = chapterNumber,
            totalChapters = chapterTotal
        )
    }
    val latestOnToggleChrome by rememberUpdatedState(onToggleChrome)
    val readerModifier = modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { latestOnToggleChrome() })
    }

    if (paginated) {
        val pagerState = rememberPagerState(
            initialPage = ReaderProgressMapper.pageIndexForProgress(restoredChapterProgress, pages.size),
            pageCount = { pages.size }
        )
        LaunchedEffect(pagerState, chapterNumber, chapterTotal, pages.size) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .drop(1)
                .collect { page ->
                    val chapterProgress = (page + 1f) / pages.size
                    val overallProgress = (
                        ((chapterNumber - 1).coerceAtLeast(0) + chapterProgress) / chapterTotal * 100
                        ).roundToInt().coerceIn(1, 100)
                    onSaveProgress(overallProgress, chapterNumber)
                }
        }

        HorizontalPager(
            state = pagerState,
            modifier = readerModifier,
            key = { it }
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "Page ${page + 1} of ${pages.size}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.foreground.copy(alpha = 0.84f),
                    textAlign = TextAlign.End
                )
                if (page == 0) {
                    Text(
                        chapter?.title ?: story.chapter.ifBlank { "Chapter $chapterNumber" },
                        fontFamily = fontFamily,
                        fontSize = readerPreferences.scaledFontSize(28f).sp,
                        lineHeight = readerPreferences.scaledLineHeight(34f, 28f).sp,
                        color = palette.foreground
                    )
                }
                Text(
                    pages[page],
                    fontFamily = fontFamily,
                    fontSize = readerPreferences.scaledFontSize(18f).sp,
                    lineHeight = readerPreferences.scaledLineHeight(31f, 18f).sp,
                    color = palette.foreground
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    } else {
        val scrollState = rememberScrollState()
        var didRestoreScroll by remember(story.id, chapterNumber) { mutableStateOf(false) }
        LaunchedEffect(scrollState, story.id, chapterNumber, chapterTotal) {
            if (didRestoreScroll) return@LaunchedEffect
            if (restoredChapterProgress == 0) {
                didRestoreScroll = true
                return@LaunchedEffect
            }
            val maxScroll = snapshotFlow { scrollState.maxValue }.first { it > 0 }
            scrollState.scrollTo(
                ReaderProgressMapper.scrollOffsetForProgress(restoredChapterProgress, maxScroll)
            )
            didRestoreScroll = true
        }
        LaunchedEffect(scrollState, chapterNumber, chapterTotal) {
            snapshotFlow {
                if (scrollState.maxValue == 0) 0
                else (scrollState.value * 100f / scrollState.maxValue).roundToInt().coerceIn(0, 100) / 5 * 5
            }
                .distinctUntilChanged()
                .filter { it in 5..100 }
                .debounce(900)
                .map { localProgress ->
                    ReaderProgressMapper.overallProgressPercent(localProgress, chapterNumber, chapterTotal)
                }
                .collect { progress -> onSaveProgress(progress, chapterNumber) }
        }

        Column(
            modifier = readerModifier
                .verticalScroll(scrollState)
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
                        modifier = Modifier.fillMaxWidth().height(460.dp).clip(MaterialTheme.shapes.small)
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
}
