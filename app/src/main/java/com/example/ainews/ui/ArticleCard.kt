package com.example.ainews.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ainews.data.Article

@Composable
fun ArticleCard(article: Article, saved: Boolean, onToggleSave: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var expanded by rememberSaveable(article.url) { mutableStateOf(false) }
    val text = article.summary ?: article.excerpt

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Card,
        contentColor = TextPrimary,
        border = BorderStroke(1.dp, Outline),
    ) {
        Column(
            Modifier
                .clickable(onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                .animateContentSize()
                .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 2.dp)
        ) {
            Row(Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(article.category.color(), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    article.sourceName.uppercase(),
                    style = MonoLabel,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val ago = timeAgo(article.publishedAt)
                if (ago.isNotEmpty()) Text("  ·  $ago", style = MonoLabel, color = TextMuted, maxLines = 1)
                Spacer(Modifier.width(12.dp))
                Spacer(Modifier.weight(1f))
                Text(article.category.label.uppercase(), style = MonoLabel, color = article.category.color(), maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 10.dp),
            )
            if (text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                if (article.summary != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(12.dp), tint = AccentAlt)
                        Spacer(Modifier.width(5.dp))
                        Text("AI SUMMARY", style = MonoLabel, color = AccentAlt)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { openUrl(context, article.url) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Read full article")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleSave) {
                    if (saved) Icon(Icons.Filled.Bookmark, "Remove from saved", tint = Accent)
                    else Icon(Icons.Outlined.BookmarkBorder, "Save for later", tint = TextMuted)
                }
                IconButton(onClick = { shareArticle(context, article) }) {
                    Icon(Icons.Outlined.Share, "Share", Modifier.size(20.dp), tint = TextMuted)
                }
            }
        }
    }
}
