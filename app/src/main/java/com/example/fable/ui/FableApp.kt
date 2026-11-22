package com.example.fable.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fable.FableApplication
import com.example.fable.ui.navigation.FableDestination
import com.example.fable.ui.navigation.FableNavBackStack
import com.example.fable.ui.navigation.FableTab
import com.example.fable.ui.screens.AccountScreen
import com.example.fable.ui.screens.EditStoryScreen
import com.example.fable.ui.screens.ExploreScreen
import com.example.fable.ui.screens.LibraryScreen
import com.example.fable.ui.screens.PublishConfirmationSheet
import com.example.fable.ui.screens.ShelfScreen
import com.example.fable.ui.screens.StoryDetailScreen
import com.example.fable.ui.screens.StoryReaderScreen
import com.example.fable.ui.screens.WriteScreen
import com.example.fable.ui.theme.FableTheme
import com.example.fable.ui.viewmodel.AccountViewModel
import com.example.fable.ui.viewmodel.CatalogViewModel
import com.example.fable.ui.viewmodel.ExploreViewModel
import com.example.fable.ui.viewmodel.ReaderViewModel
import com.example.fable.ui.viewmodel.ServerConfigViewModel
import com.example.fable.ui.viewmodel.StoryEditorViewModel
import com.example.fable.ui.viewmodel.WriterDraftViewModel
import kotlinx.coroutines.launch

private fun FableTab.icon(): ImageVector = when (this) {
    FableTab.LIBRARY -> Icons.Outlined.Book
    FableTab.EXPLORE -> Icons.Outlined.Search
    FableTab.WRITE -> Icons.Outlined.Edit
    FableTab.SHELF -> Icons.Outlined.BookmarkBorder
}

@Composable
fun FableApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as FableApplication
    val catalogViewModel: CatalogViewModel = viewModel(
        factory = CatalogViewModel.Factory(
            application.storyRepository,
            application.readingStatsRepository
        )
    )
    val exploreViewModel: ExploreViewModel = viewModel(
        factory = ExploreViewModel.Factory(application.storyRepository)
    )
    val readerViewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.Factory(
            application.storyRepository,
            application.readingStatsRepository,
            application.readerPreferencesRepository
        )
    )
    val storyEditorViewModel: StoryEditorViewModel = viewModel(
        factory = StoryEditorViewModel.Factory(application.storyRepository)
    )
    val serverConfigViewModel: ServerConfigViewModel = viewModel(
        factory = ServerConfigViewModel.Factory(
            serverConfigRepository = application.serverConfigRepository,
            checkHealth = { runCatching { application.apiService.fetchHealth() } }
        )
    )
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.Factory(application.accountRepository)
    )

    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val exploreState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
    val editorState by storyEditorViewModel.uiState.collectAsStateWithLifecycle()
    val serverConfigState by serverConfigViewModel.uiState.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()

    val writerOwnerId = accountState.localAccountId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID
    val writerDraftViewModel: WriterDraftViewModel = viewModel(
        factory = WriterDraftViewModel.Factory(application.writerDraftRepository, writerOwnerId)
    )
    val writerDraftState by writerDraftViewModel.uiState.collectAsStateWithLifecycle()

    var navBackStack by rememberSaveable(stateSaver = FableNavBackStack.Saver) {
        mutableStateOf(FableNavBackStack())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val handleBack: () -> Unit = {
        val popped = navBackStack.current
        navBackStack = navBackStack.popBack()
        when (popped) {
            is FableDestination.EditStory -> storyEditorViewModel.clearEditState()
            is FableDestination.StoryDetail -> {
                val stillInspectingStory = navBackStack.entries.any { entry ->
                    (entry is FableDestination.StoryDetail && entry.storyId == popped.storyId) ||
                        (entry is FableDestination.Reader && entry.storyId == popped.storyId)
                }
                if (!stillInspectingStory) readerViewModel.closeStory()
            }
            else -> Unit
        }
    }

    val deleteStoryById: (String) -> Unit = { storyId ->
        storyEditorViewModel.deleteStory(storyId) {
            accountViewModel.removePublishedStoryLocally(storyId)
            accountViewModel.refreshPublishedStories()
            if (readerState.selectedStory?.id == storyId) readerViewModel.closeStory()
            navBackStack = navBackStack.popStory(storyId)
            snackbarScope.launch {
                snackbarHostState.showSnackbar("Story deleted.")
            }
        }
    }

    LaunchedEffect(accountState.localAccountId) {
        catalogViewModel.setShelfOwner(accountState.localAccountId)
        readerViewModel.setActiveOwner(accountState.localAccountId)
        writerDraftViewModel.setOwner(accountState.localAccountId)
    }

    LaunchedEffect(navBackStack.current, accountState.user?.id) {
        when (val destination = navBackStack.current) {
            is FableDestination.TabRoot -> {
                if (destination.tab == FableTab.SHELF && accountState.user != null) {
                    catalogViewModel.refreshReadingStats()
                }
            }
            is FableDestination.StoryDetail -> readerViewModel.openStoryById(destination.storyId)
            is FableDestination.Reader -> readerViewModel.openStoryById(
                storyId = destination.storyId,
                startReadingChapter = destination.chapterNumber ?: 1
            )
            is FableDestination.EditStory -> {
                if (editorState.editingStory?.id != destination.storyId && !editorState.isLoadingEditStory) {
                    storyEditorViewModel.beginEditingById(destination.storyId)
                }
            }
            FableDestination.Account -> Unit
        }
    }

    LaunchedEffect(editorState.editErrorMessage, navBackStack.current) {
        val message = editorState.editErrorMessage ?: return@LaunchedEffect
        if (navBackStack.current !is FableDestination.EditStory) {
            snackbarHostState.showSnackbar(message)
            storyEditorViewModel.clearEditState()
        }
    }

    FableTheme(appAppearance = readerState.readerPreferences.appAppearance) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (navBackStack.showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    FableTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = navBackStack.currentTab == tab,
                            onClick = { navBackStack = navBackStack.selectTab(tab) },
                            icon = { Icon(tab.icon(), contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BackHandler(enabled = navBackStack.canGoBack, onBack = handleBack)
        when (val destination = navBackStack.current) {
            FableDestination.Account -> AccountScreen(
                modifier = Modifier.padding(innerPadding),
                state = accountState,
                onBack = handleBack,
                onSignIn = accountViewModel::signIn,
                onRegister = accountViewModel::register,
                onSaveProfile = accountViewModel::saveProfile,
                onRequestPasswordReset = accountViewModel::requestPasswordReset,
                onConfirmPasswordReset = accountViewModel::confirmPasswordReset,
                onSignOut = accountViewModel::signOut,
                onStorySelected = { story ->
                    readerViewModel.openStory(story)
                    navBackStack = navBackStack.openStory(story.id)
                },
                onDeleteAccount = {
                    accountViewModel.deleteAccount { deletedUserId ->
                        application.storyRepository.purgeUserData(deletedUserId)
                        application.readingStatsRepository.clearOwnerData(deletedUserId)
                        application.writerDraftRepository.clear(deletedUserId)
                        readerState.selectedStory?.let { currentStory ->
                            if (currentStory.ownerUserId == deletedUserId) {
                                readerViewModel.closeStory()
                                navBackStack = navBackStack.popStory(currentStory.id)
                            }
                        }
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("Account deleted.")
                        }
                    }
                },
                onEditStory = { story ->
                    storyEditorViewModel.beginEditing(story)
                    navBackStack = navBackStack.openEditStory(story.id)
                },
                onDeleteStory = { story -> deleteStoryById(story.id) },
                serverConfigState = serverConfigState,
                onApplyServerUrl = { rawUrl ->
                    serverConfigViewModel.applyBaseUrl(rawUrl) {
                        accountViewModel.clearSessionForServerSwitch()
                        catalogViewModel.refreshCatalog()
                    }
                },
                onResetServerUrl = {
                    serverConfigViewModel.resetToDefault {
                        accountViewModel.clearSessionForServerSwitch()
                        catalogViewModel.refreshCatalog()
                    }
                },
                onTestServerHealth = serverConfigViewModel::testConnection
            )
            is FableDestination.EditStory -> EditStoryScreen(
                modifier = Modifier.padding(innerPadding),
                story = editorState.editingStory?.takeIf { it.id == destination.storyId },
                isLoading = editorState.isLoadingEditStory,
                isSaving = editorState.isSavingEdit,
                isDeleting = editorState.isDeletingStory,
                errorMessage = editorState.editErrorMessage,
                genres = catalogState.genres,
                onBack = handleBack,
                onRetryLoad = { storyEditorViewModel.beginEditingById(destination.storyId) },
                onSave = { title, genre, chapterTitle, synopsis, content ->
                    storyEditorViewModel.updateStory(
                        storyId = destination.storyId,
                        title = title,
                        genre = genre,
                        chapterTitle = chapterTitle,
                        synopsis = synopsis,
                        content = content
                    ) {
                        accountViewModel.refreshPublishedStories()
                        navBackStack = navBackStack.popBack()
                        storyEditorViewModel.clearEditState()
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("Story updated.")
                        }
                    }
                },
                onDelete = { deleteStoryById(destination.storyId) }
            )
            is FableDestination.Reader -> {
                val activeStory = readerState.selectedStory?.takeIf { it.id == destination.storyId }
                if (activeStory == null) {
                    LoadingDestinationScaffold(
                        modifier = Modifier.padding(innerPadding),
                        title = "Reader",
                        onBack = handleBack
                    )
                } else {
                    StoryReaderScreen(
                        modifier = Modifier.padding(innerPadding),
                        story = activeStory,
                        chapters = readerState.chapters,
                        chapterNumber = readerState.selectedChapterNumber
                            ?: destination.chapterNumber
                            ?: readerState.chapters.firstOrNull()?.chapterNumber
                            ?: 1,
                        errorMessage = readerState.errorMessage,
                        readerPreferences = readerState.readerPreferences,
                        onBack = handleBack,
                        onSelectChapter = { number ->
                            readerViewModel.selectChapter(number)
                            navBackStack = navBackStack.openReader(destination.storyId, number)
                        },
                        onSaveProgress = readerViewModel::saveReadingProgress,
                        onReadingSessionEnded = readerViewModel::recordReadingSession,
                        onReaderPreferencesChanged = readerViewModel::updateReaderPreferences,
                        onResetReaderPreferences = readerViewModel::resetReaderPreferences,
                        onEnsureChapterLoaded = readerViewModel::ensureChapterLoaded
                    )
                }
            }
            is FableDestination.StoryDetail -> {
                val activeStory = readerState.selectedStory?.takeIf { it.id == destination.storyId }
                if (activeStory == null) {
                    LoadingDestinationScaffold(
                        modifier = Modifier.padding(innerPadding),
                        title = "Story",
                        onBack = handleBack
                    )
                } else {
                    val isOwned = accountState.user != null && (
                        activeStory.ownerUserId == accountState.user?.id ||
                            accountState.publishedStories.any { it.id == activeStory.id }
                        )
                    StoryDetailScreen(
                        modifier = Modifier.padding(innerPadding),
                        story = activeStory,
                        chapters = readerState.chapters,
                        isLoading = readerState.isLoadingStory,
                        errorMessage = readerState.errorMessage,
                        onBack = handleBack,
                        onRead = { chapter ->
                            val resolvedChapter = readerViewModel.startReading(chapter)
                            navBackStack = navBackStack.openReader(destination.storyId, resolvedChapter)
                        },
                        onSelectChapter = { chapter ->
                            val resolvedChapter = readerViewModel.startReading(chapter)
                            navBackStack = navBackStack.openReader(destination.storyId, resolvedChapter)
                        },
                        onToggleBookmark = { catalogViewModel.toggleBookmark(activeStory) },
                        isOwnedByCurrentUser = isOwned,
                        isDownloadingOffline = readerState.isDownloadingOffline,
                        offlineMessage = readerState.offlineMessage,
                        onDownloadOffline = readerViewModel::downloadStoryForOffline,
                        onRemoveOfflineDownload = readerViewModel::removeOfflineDownload,
                        onEditStory = {
                            storyEditorViewModel.beginEditing(activeStory)
                            navBackStack = navBackStack.openEditStory(activeStory.id)
                        },
                        onDeleteStory = { deleteStoryById(activeStory.id) }
                    )
                }
            }
            is FableDestination.TabRoot -> when (destination.tab) {
                FableTab.LIBRARY -> LibraryScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = catalogState.stories,
                    offlineStoryIds = catalogState.offlineStoryIds,
                    genres = catalogState.genres,
                    updateFeed = catalogState.updateFeed,
                    isRefreshing = catalogState.isRefreshing,
                    errorMessage = catalogState.errorMessage,
                    onRefresh = catalogViewModel::refreshCatalog,
                    onAccount = { navBackStack = navBackStack.openAccount() },
                    onStorySelected = { story ->
                        readerViewModel.openStory(story)
                        navBackStack = navBackStack.openStory(story.id)
                    },
                    onToggleBookmark = catalogViewModel::toggleBookmark
                )
                FableTab.EXPLORE -> ExploreScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = catalogState.stories,
                    offlineStoryIds = catalogState.offlineStoryIds,
                    providerStories = exploreState.providerStories,
                    genres = catalogState.genres,
                    writers = catalogState.writers,
                    isSearchingProvider = exploreState.isSearchingProvider,
                    providerErrorMessage = exploreState.providerErrorMessage,
                    onSearchProvider = { catalog, query, sort ->
                        exploreViewModel.searchProvider(catalog, query, sort)
                    },
                    onStorySelected = { story ->
                        readerViewModel.openStory(story)
                        navBackStack = navBackStack.openStory(story.id)
                    },
                    onToggleBookmark = catalogViewModel::toggleBookmark
                )
                FableTab.WRITE -> WriteScreen(
                    modifier = Modifier.padding(innerPadding),
                    expectedOwnerId = writerOwnerId,
                    draftState = writerDraftState,
                    genres = catalogState.genres,
                    isSignedIn = accountState.user != null,
                    isPublishing = editorState.isPublishing,
                    publishErrorMessage = editorState.publishErrorMessage,
                    onSignIn = { navBackStack = navBackStack.openAccount() },
                    onDraftChanged = writerDraftViewModel::updateDraft,
                    onDiscardDraft = writerDraftViewModel::discardDraft,
                    onPublish = { draft ->
                        storyEditorViewModel.publishStory(
                            title = draft.title,
                            genre = draft.genre,
                            chapterTitle = draft.chapterTitle,
                            synopsis = draft.synopsis,
                            content = draft.content,
                            onPublished = {
                                writerDraftViewModel.clearAfterPublished(writerOwnerId, draft)
                                accountViewModel.refreshPublishedStories()
                            }
                        )
                    }
                )
                FableTab.SHELF -> ShelfScreen(
                    modifier = Modifier.padding(innerPadding),
                    stories = catalogState.shelfStories,
                    offlineStoryIds = catalogState.offlineStoryIds,
                    readingStats = catalogState.readingStats,
                    showReadingStats = accountState.user != null,
                    onStorySelected = { story ->
                        readerViewModel.openStory(story)
                        navBackStack = navBackStack.openStory(story.id)
                    },
                    onToggleBookmark = catalogViewModel::toggleBookmark
                )
            }
        }

        editorState.publishedStory?.let { story ->
            PublishConfirmationSheet(
                story = story,
                onDismiss = storyEditorViewModel::dismissPublishedStory,
                onViewStory = {
                    storyEditorViewModel.dismissPublishedStory()
                    readerViewModel.openStory(story)
                    navBackStack = navBackStack.openStory(story.id)
                },
                onReturnToLibrary = {
                    storyEditorViewModel.dismissPublishedStory()
                    navBackStack = navBackStack.selectTab(FableTab.LIBRARY)
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${story.title} by ${story.author}\n\n${story.synopsis}"
                        )
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "Share story details"))
                    }.onFailure {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("Unable to open share sheet.")
                        }
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun LoadingDestinationScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

private const val GUEST_OWNER_ID = "guest"
