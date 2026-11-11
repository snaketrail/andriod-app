package com.example.ainews.data

import android.content.Context

/** Small on-device storage: cached feed, saved stories and settings. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("news", Context.MODE_PRIVATE)

    var disabledSources: Set<String>
        get() = prefs.getStringSet("disabled_sources", emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet("disabled_sources", value).apply()

    var sources: List<Source>
        get() = Source.listFromJson(prefs.getString("sources", null))
        set(value) = prefs.edit().putString("sources", Source.listToJson(value)).apply()

    /** When we last downloaded the feed. */
    var lastChecked: Long
        get() = prefs.getLong("last_checked", 0)
        set(value) = prefs.edit().putLong("last_checked", value).apply()

    /** When the daily job last built the feed. */
    var lastUpdated: Long
        get() = prefs.getLong("last_updated", 0)
        set(value) = prefs.edit().putLong("last_updated", value).apply()

    var articles: List<Article>
        get() = Article.listFromJson(prefs.getString("articles", null))
        set(value) = prefs.edit().putString("articles", Article.listToJson(value)).apply()

    var saved: List<Article>
        get() = Article.listFromJson(prefs.getString("saved", null))
        set(value) = prefs.edit().putString("saved", Article.listToJson(value)).apply()
}
