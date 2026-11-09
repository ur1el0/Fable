package com.example.fable.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.fable.ui.viewmodel.WriterDraftViewModel

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
    val writerOwnerId = accountState.localAccountId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID
    val writerDraftViewModel: WriterDraftViewModel = viewModel(
        factory = WriterDraftViewModel.Factory(application.writerDraftRepository, writerOwnerId)
    )
    val writerDraftState by writerDraftViewModel.uiState.collectAsStateWithLifecycle()
    val selectedStory = state.selectedStory
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountState.localAccountId) {
        viewModel.setShelfOwner(accountState.localAccountId)
        writerDraftViewModel.setOwner(accountState.localAccountId)
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
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        BackHandler(enabled = accountOpen, onBack = { accountOpen = false })
        BackHandler(enabled = selectedStory != null && !accountOpen, onBack = viewModel::navigateBackFromStory)
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
                    expectedOwnerId = writerOwnerId,
                    draftState = writerDraftState,
                    genres = state.genres,
                    isSignedIn = accountState.user != null,
                    isPublishing = state.isPublishing,
                    publishErrorMessage = state.publishErrorMessage,
                    onSignIn = { accountOpen = true },
                    onDraftChanged = writerDraftViewModel::updateDraft,
                    onDiscardDraft = writerDraftViewModel::discardDraft,
                    onPublish = { draft ->
                        viewModel.publishStory(
                            title = draft.title,
                            genre = draft.genre,
                            chapterTitle = draft.chapterTitle,
                            synopsis = draft.synopsis,
                            content = draft.content,
                            onPublished = {
                                writerDraftViewModel.clearAfterPublished(writerOwnerId, draft)
                            }
                        )
                    }
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

private const val GUEST_OWNER_ID = "guest"
