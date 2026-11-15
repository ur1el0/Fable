package com.example.fable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.MangaReadingMode
import com.example.fable.domain.model.Story
import com.example.fable.domain.reading.ReaderProgressMapper
import com.example.fable.ui.theme.ReaderPalette
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MangaChapterReader(
    story: Story,
    chapter: Chapter?,
    chapterNumber: Int,
    chapterTotal: Int,
    mode: MangaReadingMode,
    palette: ReaderPalette,
    onSaveProgress: (Int, Int) -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageUrls = chapter?.pageUrls.orEmpty()
    val restoredProgress = remember(story.id, chapterNumber, chapterTotal, story.progressPercent) {
        val chapterProgress = if (
            story.lastReadChapterNumber != null && story.lastReadChapterNumber != chapterNumber
        ) {
            0
        } else {
            ReaderProgressMapper.chapterProgressPercent(
                overallProgressPercent = story.progressPercent,
                chapterNumber = chapterNumber,
                totalChapters = chapterTotal
            )
        }
        ReaderProgressMapper.pageIndexForProgress(chapterProgress, pageUrls.size)
    }

    if (pageUrls.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Chapter pages are loading or unavailable.",
                color = palette.foreground.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    when (mode) {
        MangaReadingMode.WEBTOON -> WebtoonPageList(
            storyId = story.id,
            chapterNumber = chapterNumber,
            chapterTotal = chapterTotal,
            pageUrls = pageUrls,
            initialPage = restoredProgress,
            palette = palette,
            onSaveProgress = onSaveProgress,
            onToggleChrome = onToggleChrome,
            modifier = modifier
        )

        MangaReadingMode.PAGE_LTR,
        MangaReadingMode.PAGE_RTL -> PagedChapter(
            chapterNumber = chapterNumber,
            chapterTotal = chapterTotal,
            pageUrls = pageUrls,
            initialPage = restoredProgress,
            mode = mode,
            palette = palette,
            onSaveProgress = onSaveProgress,
            onToggleChrome = onToggleChrome,
            modifier = modifier
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun WebtoonPageList(
    storyId: String,
    chapterNumber: Int,
    chapterTotal: Int,
    pageUrls: List<String>,
    initialPage: Int,
    palette: ReaderPalette,
    onSaveProgress: (Int, Int) -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)
    val currentPage by remember(listState, pageUrls.size) {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    DisposableEffect(storyId, chapterNumber, chapterTotal, pageUrls.size, listState) {
        onDispose {
            webtoonProgressPercent(listState, pageUrls.size)?.let { chapterProgress ->
                latestOnSaveProgress(
                    ReaderProgressMapper.overallProgressPercent(
                        chapterProgressPercent = chapterProgress,
                        chapterNumber = chapterNumber,
                        totalChapters = chapterTotal
                    ),
                    chapterNumber
                )
            }
        }
    }

    LaunchedEffect(storyId, chapterNumber, chapterTotal, pageUrls.size, listState) {
        snapshotFlow { webtoonProgressPercent(listState, pageUrls.size) }
            .filterNotNull()
            .distinctUntilChanged()
            .drop(1)
            .debounce(PROGRESS_SAVE_DEBOUNCE_MS)
            .map { chapterProgress ->
                ReaderProgressMapper.overallProgressPercent(
                    chapterProgressPercent = chapterProgress,
                    chapterNumber = chapterNumber,
                    totalChapters = chapterTotal
                )
            }
            .collect { overallProgress ->
                latestOnSaveProgress(overallProgress, chapterNumber)
            }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .pointerInput(onToggleChrome) { detectTapGestures(onTap = { onToggleChrome() }) }
        ) {
            itemsIndexed(
                items = pageUrls,
                key = { index, pageUrl -> "$storyId-$chapterNumber-$index-$pageUrl" }
            ) { index, pageUrl ->
                AsyncImage(
                    model = pageUrl,
                    contentDescription = "Comic page ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        PageCounter(
            pageNumber = currentPage.coerceIn(1, pageUrls.size),
            pageTotal = pageUrls.size,
            palette = palette,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun PagedChapter(
    chapterNumber: Int,
    chapterTotal: Int,
    pageUrls: List<String>,
    initialPage: Int,
    mode: MangaReadingMode,
    palette: ReaderPalette,
    onSaveProgress: (Int, Int) -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageUrls.lastIndex),
        pageCount = { pageUrls.size }
    )
    val pagerScope = rememberCoroutineScope()
    val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)
    val latestOnToggleChrome by androidx.compose.runtime.rememberUpdatedState(onToggleChrome)
    val tapModifier = Modifier.pointerInput(pagerState, mode, pageUrls.size) {
        detectTapGestures { point ->
            val leftZone = point.x < size.width * TAP_ZONE_FRACTION
            val rightZone = point.x > size.width * (1f - TAP_ZONE_FRACTION)
            val isForwardTap = if (mode == MangaReadingMode.PAGE_RTL) leftZone else rightZone
            val targetPage = when {
                leftZone || rightZone -> pagerState.currentPage + if (isForwardTap) 1 else -1
                else -> null
            }
            if (targetPage == null) {
                latestOnToggleChrome()
            } else if (targetPage in pageUrls.indices) {
                pagerScope.launch { pagerState.animateScrollToPage(targetPage) }
            }
        }
    }

    DisposableEffect(pagerState, chapterNumber, chapterTotal, pageUrls.size) {
        onDispose {
            val chapterProgress = ((pagerState.currentPage + 1f) * 100f / pageUrls.size).roundToInt()
            latestOnSaveProgress(
                ReaderProgressMapper.overallProgressPercent(
                    chapterProgressPercent = chapterProgress,
                    chapterNumber = chapterNumber,
                    totalChapters = chapterTotal
                ),
                chapterNumber
            )
        }
    }

    LaunchedEffect(pagerState, chapterNumber, chapterTotal, pageUrls.size) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .map { page ->
                val chapterProgress = ((page + 1f) * 100f / pageUrls.size).roundToInt()
                ReaderProgressMapper.overallProgressPercent(
                    chapterProgressPercent = chapterProgress,
                    chapterNumber = chapterNumber,
                    totalChapters = chapterTotal
                )
            }
            .distinctUntilChanged()
            .debounce(PROGRESS_SAVE_DEBOUNCE_MS)
            .collect { page ->
                latestOnSaveProgress(page, chapterNumber)
            }
    }

    Box(modifier.fillMaxSize().background(palette.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().then(tapModifier),
            beyondViewportPageCount = 1,
            reverseLayout = mode == MangaReadingMode.PAGE_RTL,
            key = { page -> page }
        ) { page ->
            AsyncImage(
                model = pageUrls[page],
                contentDescription = "Comic page ${page + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        }
        PageCounter(
            pageNumber = pagerState.currentPage + 1,
            pageTotal = pageUrls.size,
            palette = palette,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun PageCounter(
    pageNumber: Int,
    pageTotal: Int,
    palette: ReaderPalette,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = palette.background.copy(alpha = 0.88f),
        contentColor = palette.foreground,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            "Page $pageNumber of $pageTotal",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp
        )
    }
}

private fun webtoonProgressPercent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    pageCount: Int
): Int? {
    if (pageCount <= 0) return null
    val layoutInfo = listState.layoutInfo
    val visiblePage = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
    val scrollRange = (visiblePage.size - viewportHeight).coerceAtLeast(1)
    val pageOffset = (layoutInfo.viewportStartOffset - visiblePage.offset).coerceIn(0, scrollRange)
    val withinPage = pageOffset.toFloat() / scrollRange
    return (((visiblePage.index + withinPage) / pageCount) * 100)
        .roundToInt()
        .coerceIn(0, 100)
}

private const val TAP_ZONE_FRACTION = 0.3f
private const val PROGRESS_SAVE_DEBOUNCE_MS = 700L
