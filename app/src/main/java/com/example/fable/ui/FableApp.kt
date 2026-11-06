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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fable.FableApplication
import com.example.fable.ui.screens.ExploreScreen
import com.example.fable.ui.screens.LibraryScreen
import com.example.fable.ui.screens.ShelfScreen
import com.example.fable.ui.screens.WriteScreen
import com.example.fable.ui.viewmodel.FableViewModel

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
        factory = FableViewModel.Factory(application.storyRepository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
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
    ) { innerPadding ->
        when (selectedTab) {
            0 -> LibraryScreen(
                modifier = Modifier.padding(innerPadding),
                stories = state.stories,
                genres = state.genres,
                updateFeed = state.updateFeed,
                isRefreshing = state.isRefreshing,
                errorMessage = state.errorMessage,
                onRefresh = viewModel::refreshCatalog,
                onToggleBookmark = viewModel::toggleBookmark
            )
            1 -> ExploreScreen(
                modifier = Modifier.padding(innerPadding),
                stories = state.stories,
                genres = state.genres,
                writers = state.writers,
                onToggleBookmark = viewModel::toggleBookmark
            )
            2 -> WriteScreen(modifier = Modifier.padding(innerPadding))
            else -> ShelfScreen(
                modifier = Modifier.padding(innerPadding),
                stories = state.shelfStories,
                onToggleBookmark = viewModel::toggleBookmark
            )
        }
    }
}
