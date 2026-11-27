package com.example.fable.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fable.FableApplication
import com.example.fable.ui.screens.AccountScreen
import com.example.fable.ui.screens.ExploreScreen
import com.example.fable.ui.screens.LibraryScreen
import com.example.fable.ui.screens.ShelfScreen
import com.example.fable.ui.screens.StoryDetailScreen
import com.example.fable.ui.screens.StoryReaderScreen
import com.example.fable.ui.screens.WriteScreen
import com.example.fable.ui.viewmodel.FableViewModel
import com.example.fable.ui.viewmodel.AccountViewModel

private val tabs = listOf(
    "Library" to Icons.Outlined.Book,
    "Explore" to Icons.Outlined.Search,
    "Write" to Icons.Outlined.Edit,
    "Shelf" to Icons.Outlined.BookmarkBorder
)

@Composable
fun FableApp(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as FableApplication
    val viewModel: FableViewModel = viewModel(
        factory = FableViewModel.Factory(
            application.storyRepository,
            application.readingStatsRepository,
            application.readerPreferencesRepository
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.Factory(application.accountRepository)
    )
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val selectedStory = state.selectedStory
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountState.user?.id) {
        viewModel.setShelfOwner(accountState.user?.id)
    }

    LaunchedEffect(selectedTab, accountState.user?.id) {
        if (selectedTab == 3 && accountState.user != null) viewModel.refreshReadingStats()
    }

    LaunchedEffect(state.publishNoticeMessage) {
        state.publishNoticeMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedStory == null && !accountOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, (label, icon) ->
                                val isSelected = selectedTab == index
                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(180),
                                    label = "tabTint"
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { selectedTab = index }
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = iconTint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        BackHandler(enabled = accountOpen, onBack = { accountOpen = false })
        BackHandler(enabled = selectedStory != null && !accountOpen, onBack = viewModel::navigateBackFromStory)
        val screenKey = when {
            accountOpen -> "account"
            selectedStory != null && state.isReaderOpen -> "reader-${selectedStory.id}"
            selectedStory != null -> "detail-${selectedStory.id}"
            else -> "tab-$selectedTab"
        }
        AnimatedContent(
            targetState = screenKey,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 28 })
                    .togetherWith(fadeOut(tween(150)))
            },
            label = "fableScreenTransition"
        ) { _ ->
            when {
            accountOpen -> AccountScreen(
                modifier = Modifier.padding(innerPadding),
                state = accountState,
                onBack = { accountOpen = false },
                onSignIn = accountViewModel::signIn,
                onRegister = accountViewModel::register,
                onSaveProfile = accountViewModel::saveProfile,
                onRequestPasswordReset = accountViewModel::requestPasswordReset,
                onConfirmPasswordReset = accountViewModel::confirmPasswordReset,
                onSignOut = accountViewModel::signOut,
                onStorySelected = { story ->
                    accountOpen = false
                    selectedTab = 0
                    viewModel.openStory(story)
                }
            )
            selectedStory != null && state.isReaderOpen -> StoryReaderScreen(
                modifier = Modifier.padding(innerPadding),
                story = selectedStory,
                chapters = state.chapters,
                chapterNumber = state.selectedChapterNumber ?: state.chapters.firstOrNull()?.chapterNumber ?: 1,
                errorMessage = state.errorMessage,
                readerPreferences = state.readerPreferences,
                onBack = viewModel::navigateBackFromStory,
                onSelectChapter = viewModel::selectChapter,
                onSaveProgress = viewModel::saveReadingProgress,
                onReadingSessionEnded = viewModel::recordReadingSession,
                onReaderPreferencesChanged = viewModel::updateReaderPreferences,
                onResetReaderPreferences = viewModel::resetReaderPreferences,
                onEnsureChapterLoaded = viewModel::ensureChapterLoaded
            )
            selectedStory != null -> StoryDetailScreen(
                modifier = Modifier.padding(innerPadding),
                story = selectedStory,
                chapters = state.chapters,
                isLoading = state.isLoadingStory,
                errorMessage = state.errorMessage,
                onBack = viewModel::closeStory,
                onRead = viewModel::startReading,
                onSelectChapter = viewModel::startReading,
                onToggleBookmark = { viewModel.toggleBookmark(selectedStory) }
            )
            else -> when (selectedTab) {
                0 -> LibraryScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = state.stories,
                    genres = state.genres,
                    updateFeed = state.updateFeed,
                    isRefreshing = state.isRefreshing,
                    errorMessage = state.errorMessage,
                    onRefresh = viewModel::refreshCatalog,
                    onAccount = { accountOpen = true },
                    onStorySelected = viewModel::openStory,
                    onToggleBookmark = viewModel::toggleBookmark
                )
                1 -> ExploreScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = state.stories,
                    providerStories = state.providerStories,
                    genres = state.genres,
                    writers = state.writers,
                    isSearchingProvider = state.isSearchingProvider,
                    providerErrorMessage = state.providerErrorMessage,
                    onSearchProvider = viewModel::searchProvider,
                    onStorySelected = viewModel::openStory,
                    onToggleBookmark = viewModel::toggleBookmark
                )
                2 -> WriteScreen(
                    modifier = Modifier.padding(innerPadding),
                    isSignedIn = accountState.user != null,
                    isPublishing = state.isPublishing,
                    publishErrorMessage = state.publishErrorMessage,
                    onSignIn = { accountOpen = true },
                    onPublish = viewModel::publishStory
                )
                else -> ShelfScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = state.shelfStories,
                    readingStats = state.readingStats,
                    showReadingStats = accountState.user != null,
                    onStorySelected = viewModel::openStory,
                    onToggleBookmark = viewModel::toggleBookmark
                )
            }
            }
        }
    }
}
