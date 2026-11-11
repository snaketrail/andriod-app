package com.example.ainews.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ainews.UiState

@Composable
fun SourcesScreen(state: UiState, onSetEnabled: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Sources", "${state.sources.count { it.id !in state.disabledSources }} of ${state.sources.size} active")
        if (state.sources.isEmpty()) {
            EmptyState("No sources yet", "They appear after the feed has loaded once.", Modifier.fillMaxSize())
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(state.sources, key = { it.id }) { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Open website") { openUrl(context, source.siteUrl) }
                        .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(source.name, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(source.domain, style = MonoLabel, color = TextMuted)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(12.dp), tint = TextMuted)
                        }
                    }
                    Switch(
                        checked = source.id !in state.disabledSources,
                        onCheckedChange = { onSetEnabled(source.id, it) },
                    )
                }
                HorizontalDivider(Modifier.padding(start = 20.dp), color = Outline)
            }
        }
    }
}
