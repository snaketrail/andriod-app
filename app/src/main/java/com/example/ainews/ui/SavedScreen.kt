package com.example.ainews.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ainews.UiState
import com.example.ainews.data.Article

@Composable
fun SavedScreen(state: UiState, onToggleSave: (Article) -> Unit, onBrowseFeed: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Saved", if (state.saved.size == 1) "1 story" else "${state.saved.size} stories")
        if (state.saved.isEmpty()) {
            EmptyState(
                "Nothing saved yet",
                "Tap the bookmark on a story to keep it here.",
                Modifier.fillMaxSize(),
            ) { Button(onClick = onBrowseFeed) { Text("Browse the feed") } }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.saved, key = { it.url }) { article ->
                    ArticleCard(
                        article,
                        saved = true,
                        onToggleSave = { onToggleSave(article) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
