package com.example.ainews.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where the app downloads the daily feed from. The file is produced once a day by
 * scripts/build_feed.py running on GitHub (see .github/workflows/daily-feed.yml).
 *
 */
const val FEED_URL = "https://raw.githubusercontent.com/snaketrail/andriod-app/main/data/feed.json"

class Feed(val updatedAt: Long, val sources: List<Source>, val articles: List<Article>)

object NewsRepository {
    val isConfigured: Boolean get() = FEED_URL.startsWith("https://")

    /** Downloads the ready-made feed. The app itself never talks to Gemini or to the news sites. */
    suspend fun download(): Feed = withContext(Dispatchers.IO) {
        val conn = URL(FEED_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val sources = json.getJSONArray("sources")
            val articles = json.getJSONArray("articles")
            Feed(
                updatedAt = json.optLong("updatedAt"),
                sources = (0 until sources.length()).map { Source.fromJson(sources.getJSONObject(it)) },
                articles = (0 until articles.length()).map { Article.fromJson(articles.getJSONObject(it)) },
            )
        } finally {
            conn.disconnect()
        }
    }
}
