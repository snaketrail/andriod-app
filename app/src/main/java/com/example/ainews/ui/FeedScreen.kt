package com.example.ainews.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ainews.UiState
import com.example.ainews.data.Article
import com.example.ainews.data.Category
import com.example.ainews.data.categoryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(state: UiState, onRefresh: () -> Unit, onToggleSave: (Article) -> Unit) {
    var filterName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = categoryOf(filterName)
    val fromActiveSources = state.articles.filter { it.sourceId !in state.disabledSources }
    val visible = if (filter == null) fromActiveSources else fromActiveSources.filter { it.category == filter }
    val listState = rememberLazyListState()
    LaunchedEffect(filterName) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Latest",
            subtitle = when {
                state.loading -> "checking for new stories…"
                state.lastUpdated == 0L -> "not updated yet"
                else -> "${fromActiveSources.size} stories · updated ${timeAgo(state.lastUpdated)}"
            },
        ) {
            IconButton(onClick = onRefresh, enabled = !state.loading) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "Refresh")
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(selected = filter == null, onClick = { filterName = null }, label = { Text("All") })
            }
            items(Category.entries) { category ->
                FilterChip(
                    selected = filter == category,
                    onClick = { filterName = if (filter == category) null else category.name },
                    label = { Text(category.label) },
                )
            }
        }

        PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (visible.isEmpty()) {
                    item {
                        when {
                            state.loading -> EmptyState(
                                "Fetching the latest…", "One moment.", Modifier.fillParentMaxSize(),
                            )
                            filter != null && fromActiveSources.isNotEmpty() -> EmptyState(
                                "Nothing here right now",
                                "No ${filter.label} stories in the current feed.",
                                Modifier.fillParentMaxSize(),
                            ) { TextButton(onClick = { filterName = null }) { Text("Show all") } }
                            else -> EmptyState(
                                "No stories yet",
                                "Pull down or tap refresh to load the latest AI news.",
                                Modifier.fillParentMaxSize(),
                            ) { Button(onClick = onRefresh) { Text("Refresh") } }
                        }
                    }
                }
                items(visible, key = { it.url }) { article ->
                    ArticleCard(article, saved = article.url in state.savedUrls, onToggleSave = { onToggleSave(article) })
                }
            }
        }
    }
}
