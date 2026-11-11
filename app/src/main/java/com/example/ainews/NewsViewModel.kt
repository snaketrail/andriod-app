package com.example.ainews

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainews.data.Article
import com.example.ainews.data.NewsRepository
import com.example.ainews.data.Source
import com.example.ainews.data.Store
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val articles: List<Article> = emptyList(),
    val saved: List<Article> = emptyList(),
    val sources: List<Source> = emptyList(),
    val disabledSources: Set<String> = emptySet(),
    val loading: Boolean = false,
    /** When the daily job last built the feed. */
    val lastUpdated: Long = 0,
) {
    val savedUrls: Set<String> = saved.mapTo(HashSet()) { it.url }
}

class NewsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    private val _state = MutableStateFlow(
        UiState(
            articles = store.articles,
            saved = store.saved,
            sources = store.sources,
            disabledSources = store.disabledSources,
            lastUpdated = store.lastUpdated,
        )
    )
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** One-off messages shown as a snackbar. */
    val messages = _messages.asSharedFlow()

    init {
        // The feed changes once a day, so checking at most once an hour on launch is plenty.
        val stale = System.currentTimeMillis() - store.lastChecked > 60 * 60 * 1000
        if (_state.value.articles.isEmpty() || stale) refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        if (!NewsRepository.isConfigured) {
            _messages.tryEmit("Feed address is not set yet (see NewsRepository.kt)")
            return
        }
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val feed = NewsRepository.download()
                val isNew = feed.updatedAt != store.lastUpdated
                store.articles = feed.articles
                store.sources = feed.sources
                store.lastUpdated = feed.updatedAt
                store.lastChecked = System.currentTimeMillis()
                _state.update {
                    it.copy(articles = feed.articles, sources = feed.sources, lastUpdated = feed.updatedAt, loading = false)
                }
                if (!isNew) _messages.tryEmit("You're up to date. New stories arrive once a day.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false) }
                _messages.tryEmit("Couldn't load news. Check your connection.")
            }
        }
    }

    fun toggleSaved(article: Article) {
        val current = store.saved
        store.saved = if (current.any { it.url == article.url }) current.filter { it.url != article.url }
        else listOf(article) + current
        _state.update { it.copy(saved = store.saved) }
    }

    fun setSourceEnabled(id: String, enabled: Boolean) {
        _state.update { it.copy(disabledSources = if (enabled) it.disabledSources - id else it.disabledSources + id) }
        store.disabledSources = _state.value.disabledSources
    }

    fun clearCache() {
        store.articles = emptyList()
        store.lastUpdated = 0
        store.lastChecked = 0
        _state.update { it.copy(articles = emptyList(), lastUpdated = 0) }
        _messages.tryEmit("Cached stories cleared")
    }
}
