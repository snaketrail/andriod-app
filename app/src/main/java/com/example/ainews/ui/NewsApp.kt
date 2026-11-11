package com.example.ainews.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ainews.NewsViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    FEED("Feed", Icons.Outlined.Newspaper),
    SAVED("Saved", Icons.Outlined.BookmarkBorder),
    SOURCES("Sources", Icons.Outlined.RssFeed),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
fun NewsApp(vm: NewsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.FEED) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = tab != Tab.FEED) { tab = Tab.FEED }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(selectedTextColor = Accent),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.FEED -> FeedScreen(state, onRefresh = vm::refresh, onToggleSave = vm::toggleSaved)
                Tab.SAVED -> SavedScreen(state, onToggleSave = vm::toggleSaved, onBrowseFeed = { tab = Tab.FEED })
                Tab.SOURCES -> SourcesScreen(state, onSetEnabled = vm::setSourceEnabled)
                Tab.SETTINGS -> SettingsScreen(state, onClearCache = vm::clearCache)
            }
        }
    }
}
