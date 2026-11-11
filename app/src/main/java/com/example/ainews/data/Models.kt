package com.example.ainews.data

import org.json.JSONArray
import org.json.JSONObject

enum class Category(val label: String) {
    LOCAL("Local AI"),
    MODELS("New models"),
    TOOLS("Tools"),
    RESEARCH("Research"),
    INDUSTRY("Industry"),
}

data class Source(val id: String, val name: String, val siteUrl: String) {
    val domain: String get() = siteUrl.substringAfter("://").removePrefix("www.").substringBefore('/')

    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("siteUrl", siteUrl)

    companion object {
        fun fromJson(o: JSONObject) = Source(o.getString("id"), o.getString("name"), o.getString("siteUrl"))

        fun listToJson(list: List<Source>): String =
            JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }.toString()

        fun listFromJson(json: String?): List<Source> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class Article(
    val url: String,
    val title: String,
    val sourceId: String,
    val sourceName: String,
    val publishedAt: Long,
    /** Text taken from the site itself. */
    val excerpt: String,
    /** AI-written summary, null if the story has none yet. */
    val summary: String?,
    val category: Category,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("url", url)
        .put("title", title)
        .put("sourceId", sourceId)
        .put("sourceName", sourceName)
        .put("publishedAt", publishedAt)
        .put("excerpt", excerpt)
        .put("summary", summary ?: JSONObject.NULL)
        .put("category", category.name)

    companion object {
        fun fromJson(o: JSONObject) = Article(
            url = o.getString("url"),
            title = o.getString("title"),
            sourceId = o.getString("sourceId"),
            sourceName = o.getString("sourceName"),
            publishedAt = o.optLong("publishedAt"),
            excerpt = o.optString("excerpt"),
            summary = if (o.isNull("summary")) null else o.getString("summary"),
            category = categoryOf(o.optString("category")) ?: Category.INDUSTRY,
        )

        fun listToJson(list: List<Article>): String =
            JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }.toString()

        fun listFromJson(json: String?): List<Article> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

fun categoryOf(name: String?): Category? =
    Category.entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
