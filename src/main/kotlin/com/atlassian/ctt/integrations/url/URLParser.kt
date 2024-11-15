package com.atlassian.ctt.integrations.url

import java.net.URISyntaxException

data class URLParts(
    val baseURL: String,
    val apiPath: String,
    val pathParams: List<String>,
    val queryParams: List<Pair<String, String>>,
) {
    override fun toString(): String =
        baseURL + "/" + apiPath +
            pathParams.takeIf { it.isNotEmpty() }?.joinToString(separator = "/", prefix = "/") { it }.orEmpty() +
            queryParams.takeIf { it.isNotEmpty() }?.joinToString(separator = "&", prefix = "?") { it.first + "=" + it.second }.orEmpty()
}

/* URL Parser interface. Parses the URL into following parts
* 1. Base URL
* 2. API Path
* 3. List of path parameters
* 4. List of query parameters
* */
interface URLParser {
    @Throws(URISyntaxException::class)
    fun parseURL(url: String): URLParts
}
